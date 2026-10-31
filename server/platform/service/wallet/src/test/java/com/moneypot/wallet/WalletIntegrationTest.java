package com.moneypot.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypot.inbox.InboxListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the wallet against a real (H2) database: the member API, and the pot events that move money
 * (fed to the inbox the same way RabbitMQ delivers them).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WalletIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();

   @DynamicPropertySource
   static void jwt(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   JdbcTemplate jdbc;

   @Autowired
   InboxListener inboxListener;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
   }

   private Reply call(String method, String path, String body, String token) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
      if (token != null) {
         request.header("Authorization", "Bearer " + token);
      }
      request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json);
   }

   private long balanceOf(UUID user) throws Exception {
      return call("GET", "/api/wallet", null, TestTokens.member(user)).body().path("balance").asLong();
   }

   private void deposit(UUID user, long paise) throws Exception {
      assertThat(call("POST", "/api/wallet/deposit", "{\"amount\":" + paise + "}", TestTokens.member(user)).status()).isEqualTo(200);
   }

   private UUID newPot() {
      UUID pot = UUID.randomUUID();
      inboxListener.listen("msg-" + UUID.randomUUID(), "pot.created", "{\"potId\":\"" + pot + "\",\"name\":\"Friends\",\"version\":0}");
      return pot;
   }

   private void requestTransfer(UUID pot, UUID round, String reference, String kind, UUID user, long amount) {
      requestTransfer("msg-" + UUID.randomUUID(), pot, round, reference, kind, user, amount);
   }

   private void requestTransfer(String messageId, UUID pot, UUID round, String reference, String kind, UUID user, long amount) {
      inboxListener.listen(messageId, "pot.transfer-requested", "{\"potId\":\"" + pot + "\",\"roundId\":\"" + round
            + "\",\"reference\":\"" + reference + "\",\"kind\":\"" + kind + "\",\"userId\":\"" + user + "\",\"amount\":" + amount + "}");
   }

   private long potBalance(UUID pot) {
      return jdbc.queryForObject("select balance from accounts where owner_id = ?", Long.class, pot);
   }

   private int answers(String reference, String signature) {
      return jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'wallet' and payload like ? and payload like ?",
            Integer.class, "%" + reference + "%", "%" + signature + "%");
   }

   @Test
   void aMemberOpensAWalletOnceAndTopsItUp() throws Exception {
      UUID user = UUID.randomUUID();

      Reply first = call("POST", "/api/wallet", null, TestTokens.member(user));
      Reply again = call("POST", "/api/wallet", null, TestTokens.member(user));
      deposit(user, 250_000);

      assertThat(first.status()).isEqualTo(200);
      assertThat(again.body().path("accountId").asText()).isEqualTo(first.body().path("accountId").asText());
      assertThat(balanceOf(user)).isEqualTo(250_000);
   }

   @Test
   void theWalletNeedsASignedInMember() throws Exception {
      assertThat(call("GET", "/api/wallet", null, null).status()).isEqualTo(401);
   }

   @Test
   void aContributionMovesMoneyIntoThePotAndIsAnswered() throws Exception {
      UUID user = UUID.randomUUID();
      UUID pot = newPot();
      deposit(user, 800_000);

      requestTransfer(pot, UUID.randomUUID(), "contribution-1", "CONTRIBUTION", user, 500_000);

      assertThat(balanceOf(user)).isEqualTo(300_000);
      assertThat(potBalance(pot)).isEqualTo(500_000);
      assertThat(answers("contribution-1", "transfer-completed")).isEqualTo(1);
   }

   @Test
   void aTransferWithoutEnoughMoneyIsRejectedAndChangesNothing() throws Exception {
      UUID user = UUID.randomUUID();
      UUID pot = newPot();
      deposit(user, 100_000);

      requestTransfer(pot, UUID.randomUUID(), "contribution-poor", "CONTRIBUTION", user, 500_000);

      assertThat(balanceOf(user)).isEqualTo(100_000);
      assertThat(potBalance(pot)).isZero();
      assertThat(answers("contribution-poor", "transfer-rejected")).isEqualTo(1);
      assertThat(answers("contribution-poor", "transfer-completed")).isZero();
   }

   @Test
   void theSameReferenceMovesMoneyOnlyOnce() throws Exception {
      UUID user = UUID.randomUUID();
      UUID pot = newPot();
      UUID round = UUID.randomUUID();
      deposit(user, 800_000);

      requestTransfer("same-message", pot, round, "contribution-twice", "CONTRIBUTION", user, 500_000);
      requestTransfer("same-message", pot, round, "contribution-twice", "CONTRIBUTION", user, 500_000);
      requestTransfer(pot, round, "contribution-twice", "CONTRIBUTION", user, 500_000);

      assertThat(balanceOf(user)).isEqualTo(300_000);
      assertThat(potBalance(pot)).isEqualTo(500_000);
   }

   @Test
   void aPayoutAndADividendComeOutOfThePotAndTheLedgerStaysInBalance() throws Exception {
      UUID winner = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      UUID pot = newPot();
      UUID round = UUID.randomUUID();
      deposit(winner, 500_000);
      deposit(other, 500_000);
      requestTransfer(pot, round, "c-winner", "CONTRIBUTION", winner, 500_000);
      requestTransfer(pot, round, "c-other", "CONTRIBUTION", other, 500_000);

      requestTransfer(pot, round, "payout-1", "PAYOUT", winner, 800_000);
      requestTransfer(pot, round, "dividend-1", "DIVIDEND", other, 200_000);

      assertThat(balanceOf(winner)).isEqualTo(800_000);
      assertThat(balanceOf(other)).isEqualTo(200_000);
      assertThat(potBalance(pot)).isZero();
      // every credit has a matching debit, so the whole ledger adds up to zero
      assertThat(jdbc.queryForObject(
            "select coalesce(sum(case when type = 'CREDIT' then amount else -amount end), 0) from ledger_entries "
                  + "where kind <> 'DEPOSIT'", Long.class)).isZero();
   }

   @Test
   void theLedgerShowsAMemberTheirHistoryNewestFirst() throws Exception {
      UUID user = UUID.randomUUID();
      UUID pot = newPot();
      deposit(user, 800_000);
      requestTransfer(pot, UUID.randomUUID(), "contribution-ledger", "CONTRIBUTION", user, 500_000);

      Reply ledger = call("GET", "/api/wallet/ledger?page=0&size=10", null, TestTokens.member(user));

      assertThat(ledger.status()).isEqualTo(200);
      assertThat(ledger.body().path("items")).hasSize(2);
      assertThat(ledger.body().path("items").get(0).path("kind").asText()).isEqualTo("CONTRIBUTION");
      assertThat(ledger.body().path("items").get(0).path("balanceAfter").asLong()).isEqualTo(300_000);
      assertThat(ledger.body().path("items").get(1).path("kind").asText()).isEqualTo("DEPOSIT");
   }

   @Test
   void aRefundGivesTheContributionBackAndAnsweredTheSameWay() throws Exception {
      UUID user = UUID.randomUUID();
      UUID pot = newPot();
      deposit(user, 800_000);
      requestTransfer(pot, UUID.randomUUID(), "contribution-refund", "CONTRIBUTION", user, 500_000);

      requestTransfer(pot, UUID.randomUUID(), "refund-1", "REFUND", user, 500_000);

      assertThat(balanceOf(user)).isEqualTo(800_000);
      assertThat(potBalance(pot)).isZero();
      assertThat(answers("refund-1", "transfer-completed")).isEqualTo(1);
      Reply ledger = call("GET", "/api/wallet/ledger?page=0&size=10", null, TestTokens.member(user));
      assertThat(ledger.body().path("items").get(0).path("kind").asText()).isEqualTo("REFUND");
   }
}
