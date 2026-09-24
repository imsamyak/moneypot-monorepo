package com.moneypot.pot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypot.inbox.InboxListener;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.service.RoundSettlementScheduler;
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
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the pot against a real (H2) database. The tests play the wallet: they read the transfers the pot
 * asked for from the outbox table and answer them through the inbox, the same way RabbitMQ would deliver them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PotIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final long CONTRIBUTION = 100_000; // Rs 1,000
   private static final long POT = 3 * CONTRIBUTION;

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

   @Autowired
   LoadRoundPort loadRoundPort;

   @Autowired
   SettleRoundUseCase settleRoundUseCase;

   /** The scheduler is switched off in the tests (pot.settlement.enabled=false), so the test runs it by hand. */
   private RoundSettlementScheduler scheduler;

   private final HttpClient http = HttpClient.newHttpClient();
   private final Set<String> answered = new HashSet<>();

   @org.junit.jupiter.api.BeforeEach
   void createScheduler() {
      scheduler = new RoundSettlementScheduler(loadRoundPort, settleRoundUseCase);
   }

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

   private Reply as(UUID user, String method, String path, String body) throws Exception {
      return call(method, path, body, TestTokens.member(user));
   }

   private UUID createPot(UUID owner) throws Exception {
      Reply created = as(owner, "POST", "/api/pots",
            "{\"name\":\"Friends\",\"memberCount\":3,\"contribution\":" + CONTRIBUTION + ",\"minBidPercent\":60,\"bidWindowSeconds\":3600}");
      assertThat(created.status()).isEqualTo(201);
      return UUID.fromString(created.body().path("id").asText());
   }

   /** A started pot of three members: the owner and two others, round 1 collecting. */
   private UUID startedPot(UUID a, UUID b, UUID c) throws Exception {
      UUID pot = createPot(a);
      assertThat(as(b, "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(200);
      assertThat(as(c, "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(200);
      assertThat(as(a, "POST", "/api/pots/" + pot + "/start", null).status()).isEqualTo(200);
      return pot;
   }

   private JsonNode pot(UUID viewer, UUID pot) throws Exception {
      return as(viewer, "GET", "/api/pots/" + pot, null).body();
   }

   /**
    * The transfers the pot asked the wallet for that nobody has answered yet. A retried contribution repeats its
    * reference, so only the latest request per reference counts.
    */
   private List<JsonNode> pendingRequests(UUID pot) throws Exception {
      List<String> payloads = jdbc.queryForList(
            "select payload from outbox_events where aggregate_type = 'pot' and aggregate_id = ? and payload like '%transfer-requested%' order by created_at",
            String.class, pot.toString());
      java.util.Map<String, JsonNode> latest = new java.util.LinkedHashMap<>();
      for (String payload : payloads) {
         JsonNode data = JSON.readTree(payload).path("data");
         latest.put(data.path("reference").asText(), data);
      }
      return latest.entrySet().stream()
            .filter(entry -> !answered.contains(entry.getKey()))
            .map(java.util.Map.Entry::getValue)
            .toList();
   }

   private void answer(JsonNode request, boolean completed) {
      String reference = request.path("reference").asText();
      answered.add(reference);
      String common = "\"potId\":\"" + request.path("potId").asText() + "\",\"roundId\":\"" + request.path("roundId").asText()
            + "\",\"reference\":\"" + reference + "\",\"kind\":\"" + request.path("kind").asText()
            + "\",\"userId\":\"" + request.path("userId").asText() + "\",\"amount\":" + request.path("amount").asLong();
      if (completed) {
         inboxListener.listen("wallet-" + UUID.randomUUID(), "wallet.transfer-completed", "{" + common + "}");
      } else {
         inboxListener.listen("wallet-" + UUID.randomUUID(), "wallet.transfer-rejected", "{" + common + ",\"reason\":\"Insufficient funds\"}");
      }
   }

   /** Plays a wallet that has the money: answers every transfer the pot is waiting on, once. */
   private void walletPaysEverything(UUID pot) throws Exception {
      pendingRequests(pot).forEach(request -> answer(request, true));
   }

   private long sumOf(UUID pot, String kind) throws Exception {
      long sum = 0;
      for (String payload : jdbc.queryForList(
            "select payload from outbox_events where aggregate_type = 'pot' and aggregate_id = ? and payload like '%transfer-requested%'",
            String.class, pot.toString())) {
         JsonNode data = JSON.readTree(payload).path("data");
         if (data.path("kind").asText().equals(kind)) {
            sum += data.path("amount").asLong();
         }
      }
      return sum;
   }

   // ------------------------------------------------------------------ joining and starting

   @Test
   void creatingAPotAnnouncesItAndMakesTheOwnerTheFirstMember() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID pot = createPot(owner);

      JsonNode created = pot(owner, pot);
      assertThat(created.path("status").asText()).isEqualTo("OPEN");
      assertThat(created.path("members")).hasSize(1);
      assertThat(created.path("potAmount").asLong()).isEqualTo(POT);
      assertThat(jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ? and payload like '%created%'",
            Integer.class, pot.toString())).isEqualTo(1);
   }

   @Test
   void aMemberJoinsOnceAndOnlyWhileThereIsRoom() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID pot = createPot(owner);
      UUID b = UUID.randomUUID();

      assertThat(as(b, "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(200);
      assertThat(as(b, "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(422);
      assertThat(as(owner, "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(422);
      assertThat(as(UUID.randomUUID(), "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(200);
      assertThat(as(UUID.randomUUID(), "POST", "/api/pots/" + pot + "/join", null).status()).isEqualTo(422);
   }

   @Test
   void onlyTheOwnerStartsThePotAndOnlyWhenItIsFull() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID pot = createPot(owner);
      as(b, "POST", "/api/pots/" + pot + "/join", null);

      assertThat(as(owner, "POST", "/api/pots/" + pot + "/start", null).status()).isEqualTo(422);

      as(UUID.randomUUID(), "POST", "/api/pots/" + pot + "/join", null);
      assertThat(as(b, "POST", "/api/pots/" + pot + "/start", null).status()).isEqualTo(403);
      assertThat(as(owner, "POST", "/api/pots/" + pot + "/start", null).status()).isEqualTo(200);
   }

   @Test
   void startingAsksEveryMemberForTheirContribution() throws Exception {
      UUID a = UUID.randomUUID();
      UUID pot = startedPot(a, UUID.randomUUID(), UUID.randomUUID());

      assertThat(pendingRequests(pot)).hasSize(3).allSatisfy(request -> {
         assertThat(request.path("kind").asText()).isEqualTo("CONTRIBUTION");
         assertThat(request.path("amount").asLong()).isEqualTo(CONTRIBUTION);
      });
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("COLLECTING");
   }

   // ------------------------------------------------------------------ collecting and bidding

   @Test
   void biddingOpensOnlyWhenEveryContributionIsPaid() throws Exception {
      UUID a = UUID.randomUUID();
      UUID pot = startedPot(a, UUID.randomUUID(), UUID.randomUUID());
      List<JsonNode> requests = pendingRequests(pot);

      answer(requests.get(0), true);
      answer(requests.get(1), true);
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("COLLECTING");

      answer(requests.get(2), true);
      JsonNode round = pot(a, pot).path("round");
      assertThat(round.path("status").asText()).isEqualTo("BIDDING");
      assertThat(round.path("biddingEndsAt").asText()).isNotBlank();
   }

   @Test
   void aRejectedContributionCanBePaidAgainAndKeepsItsReference() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID pot = startedPot(a, b, UUID.randomUUID());
      List<JsonNode> requests = pendingRequests(pot);
      JsonNode poor = requests.stream().filter(r -> r.path("userId").asText().equals(b.toString())).findFirst().orElseThrow();

      requests.stream().filter(r -> r != poor).forEach(r -> answer(r, true));
      answer(poor, false);
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("COLLECTING");

      assertThat(as(a, "POST", "/api/pots/" + pot + "/contribution", null).status()).isEqualTo(422); // a already paid
      answered.remove(poor.path("reference").asText()); // the member asks again, so the wallet is waiting again
      assertThat(as(b, "POST", "/api/pots/" + pot + "/contribution", null).status()).isEqualTo(200);
      List<JsonNode> again = pendingRequests(pot);
      assertThat(again).hasSize(1);
      assertThat(again.get(0).path("reference").asText()).isEqualTo(poor.path("reference").asText());

      answer(again.get(0), true);
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("BIDDING");
   }

   @Test
   void bidsFollowTheRulesOfTheRound() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID outsider = UUID.randomUUID();
      UUID pot = startedPot(a, b, UUID.randomUUID());
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}").status()).isEqualTo(422); // still collecting
      walletPaysEverything(pot);

      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":179999}").status()).isEqualTo(422); // below 60%
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":300001}").status()).isEqualTo(422); // above the pot
      assertThat(as(outsider, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}").status()).isEqualTo(403);
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}").status()).isEqualTo(200);
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}").status()).isEqualTo(422); // not lower
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":260000}").status()).isEqualTo(422); // higher
      assertThat(as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":240000}").status()).isEqualTo(200); // lowered

      JsonNode seen = pot(a, pot).path("round").path("bids");
      assertThat(seen).hasSize(1);
      assertThat(seen.get(0).path("amount").asLong()).isEqualTo(240_000);
      assertThat(pot(outsider, pot).path("round").path("bids")).isEmpty(); // bids are for members only
   }

   // ------------------------------------------------------------------ settling

   @Test
   void theLowestBidWinsAndTheRestGoesBackToTheOthersAsDividends() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID c = UUID.randomUUID();
      UUID pot = startedPot(a, b, c);
      walletPaysEverything(pot);
      as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}");
      as(b, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":240000}");
      as(c, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":270000}");

      assertThat(as(c, "POST", "/api/pots/" + pot + "/settle", null).status()).isEqualTo(403); // not the owner
      assertThat(as(a, "POST", "/api/pots/" + pot + "/settle", null).status()).isEqualTo(200);

      // b wins Rs 2,400; the Rs 600 left is shared by a and c: Rs 300 each
      List<JsonNode> requests = pendingRequests(pot);
      assertThat(requests).hasSize(3);
      JsonNode payout = requests.stream().filter(r -> r.path("kind").asText().equals("PAYOUT")).findFirst().orElseThrow();
      assertThat(payout.path("userId").asText()).isEqualTo(b.toString());
      assertThat(payout.path("amount").asLong()).isEqualTo(240_000);
      assertThat(requests.stream().filter(r -> r.path("kind").asText().equals("DIVIDEND")).map(r -> r.path("amount").asLong()))
            .containsExactly(30_000L, 30_000L);
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("SETTLING");
   }

   @Test
   void theSchedulerSettlesARoundWhoseBiddingTimeIsOver() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID pot = startedPot(a, b, UUID.randomUUID());
      walletPaysEverything(pot);
      as(b, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":200000}");

      scheduler.settleRoundsWhoseBiddingEnded();
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("BIDDING"); // time is not over

      jdbc.update("update pot_rounds set bidding_ends_at = ? where pot_id = ?", java.sql.Timestamp.from(Instant.now().minusSeconds(5)), pot);
      scheduler.settleRoundsWhoseBiddingEnded();

      JsonNode round = pot(a, pot).path("round");
      assertThat(round.path("status").asText()).isEqualTo("SETTLING");
      assertThat(round.path("winnerId").asText()).isEqualTo(b.toString());
   }

   @Test
   void aWholePotRunsToTheEndAndEveryMemberWinsExactlyOnce() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID c = UUID.randomUUID();
      UUID pot = startedPot(a, b, c);

      // round 1: b wins with the lowest bid
      walletPaysEverything(pot);
      as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":250000}");
      as(b, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":240000}");
      as(a, "POST", "/api/pots/" + pot + "/settle", null);
      walletPaysEverything(pot);

      JsonNode afterRound1 = pot(a, pot);
      assertThat(afterRound1.path("currentRound").asInt()).isEqualTo(2);
      assertThat(afterRound1.path("round").path("status").asText()).isEqualTo("COLLECTING");
      assertThat(afterRound1.path("history").get(0).path("winnerId").asText()).isEqualTo(b.toString());

      // round 2: b has won, so b cannot bid; c wins against a
      walletPaysEverything(pot);
      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("BIDDING");
      assertThat(as(b, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":200000}").status()).isEqualTo(422);
      as(a, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":280000}");
      as(c, "POST", "/api/pots/" + pot + "/bids", "{\"amount\":190000}");
      as(a, "POST", "/api/pots/" + pot + "/settle", null);
      walletPaysEverything(pot);

      // round 3: only a is left, so once the contributions are in, a is paid the whole pot with no bidding
      walletPaysEverything(pot);
      JsonNode last = pot(a, pot);
      assertThat(last.path("currentRound").asInt()).isEqualTo(3);
      assertThat(last.path("round").path("status").asText()).isEqualTo("SETTLING");
      assertThat(last.path("round").path("winnerId").asText()).isEqualTo(a.toString());
      assertThat(last.path("round").path("payout").asLong()).isEqualTo(POT);
      walletPaysEverything(pot);

      JsonNode finished = pot(a, pot);
      assertThat(finished.path("status").asText()).isEqualTo("COMPLETED");
      assertThat(finished.path("members")).allSatisfy(member -> assertThat(member.path("won").asBoolean()).isTrue());

      // the money adds up: contributions in = payouts + dividends out
      assertThat(sumOf(pot, "CONTRIBUTION")).isEqualTo(3 * POT);
      assertThat(sumOf(pot, "PAYOUT") + sumOf(pot, "DIVIDEND")).isEqualTo(3 * POT);
   }

   @Test
   void aRepeatedAnswerFromTheWalletChangesNothing() throws Exception {
      UUID a = UUID.randomUUID();
      UUID pot = startedPot(a, UUID.randomUUID(), UUID.randomUUID());
      List<JsonNode> requests = pendingRequests(pot);
      requests.forEach(request -> answer(request, true));
      int before = jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ?", Integer.class, pot.toString());

      requests.forEach(request -> answer(request, true));

      assertThat(pot(a, pot).path("round").path("status").asText()).isEqualTo("BIDDING");
      assertThat(jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ?", Integer.class, pot.toString())).isEqualTo(before);
   }

   @Test
   void potsAreListedForTheirMembersAndForThoseWhoWantToJoin() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID pot = createPot(owner);

      JsonNode open = as(UUID.randomUUID(), "GET", "/api/pots?scope=open", null).body().path("items");
      JsonNode mine = as(owner, "GET", "/api/pots?scope=mine", null).body().path("items");
      JsonNode notMine = as(UUID.randomUUID(), "GET", "/api/pots?scope=mine", null).body().path("items");

      assertThat(open.findValuesAsText("id")).contains(pot.toString());
      assertThat(mine).hasSize(1);
      assertThat(mine.get(0).path("joined").asInt()).isEqualTo(1);
      assertThat(notMine).isEmpty();
   }

   @Test
   void everythingNeedsASignedInMember() throws Exception {
      assertThat(call("GET", "/api/pots", null, null).status()).isEqualTo(401);
      assertThat(call("POST", "/api/pots", "{}", null).status()).isEqualTo(401);
   }
}
