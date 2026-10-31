package com.moneypot.pot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneypot.inbox.InboxListener;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.in.usecase.StartPotUseCase;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.service.PotStartScheduler;
import com.moneypot.pot.service.RoundSettlementScheduler;
import org.junit.jupiter.api.BeforeEach;
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
import java.net.http.WebSocket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the pot against a real (H2) database. The tests play the wallet: they read the transfers the pot
 * asked for from the outbox table and answer them through the inbox, the same way RabbitMQ would deliver them. They also
 * play the clock: moving the bidding start time into the past and running the schedulers by hand.
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
   LoadPotPort loadPotPort;

   @Autowired
   SettleRoundUseCase settleRoundUseCase;

   @Autowired
   StartPotUseCase startPotUseCase;

   /** The schedulers are switched off in the tests (pot.start.enabled and pot.settlement.enabled are false), so the tests run them by hand. */
   private RoundSettlementScheduler settlementScheduler;
   private PotStartScheduler startScheduler;

   private final HttpClient http = HttpClient.newHttpClient();
   private final Set<String> answered = new HashSet<>();

   @BeforeEach
   void createSchedulers() {
      settlementScheduler = new RoundSettlementScheduler(loadRoundPort, settleRoundUseCase);
      startScheduler = new PotStartScheduler(loadPotPort, startPotUseCase);
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

   // ------------------------------------------------------------------ helpers: pots, wallet and clock

   private record Created(UUID id, String code) {
   }

   private Created createPot(UUID owner) throws Exception {
      String startsAt = Instant.now().plusSeconds(3600).toString();
      Reply created = as(owner, "POST", "/api/pots",
            "{\"name\":\"Friends\",\"contribution\":" + CONTRIBUTION + ",\"minBidPercent\":60,\"bidWindowSeconds\":3600,\"startsAt\":\"" + startsAt + "\"}");
      assertThat(created.status()).isEqualTo(201);
      return new Created(UUID.fromString(created.body().path("id").asText()), created.body().path("inviteCode").asText());
   }

   private Reply join(UUID user, Created pot) throws Exception {
      return as(user, "POST", "/api/pots/" + pot.id() + "/join", "{\"inviteCode\":\"" + pot.code() + "\"}");
   }

   private JsonNode pot(UUID viewer, UUID pot) throws Exception {
      return as(viewer, "GET", "/api/pots/" + pot, null).body();
   }

   /** The transfers the pot asked the wallet for that nobody has answered yet; a retry repeats its reference, so only the latest request per reference counts. */
   private List<JsonNode> pendingRequests(UUID pot) throws Exception {
      List<String> payloads = jdbc.queryForList(
            "select payload from outbox_events where aggregate_type = 'pot' and aggregate_id = ? and payload like '%transfer-requested%' order by created_at",
            String.class, pot.toString());
      Map<String, JsonNode> latest = new LinkedHashMap<>();
      for (String payload : payloads) {
         JsonNode data = JSON.readTree(payload).path("data");
         latest.put(data.path("reference").asText(), data);
      }
      return latest.entrySet().stream()
            .filter(entry -> !answered.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .toList();
   }

   private List<JsonNode> pending(UUID pot, String kind) throws Exception {
      return pendingRequests(pot).stream().filter(r -> r.path("kind").asText().equals(kind)).toList();
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

   /** Moves the bidding start time into the past and lets the start scheduler run. */
   private void startTimeComes(UUID pot) {
      jdbc.update("update pots set starts_at = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(5)), pot);
      startScheduler.startPotsWhoseTimeHasCome();
   }

   /** A pot of three members who all paid, with the start time passed: round 1 is bidding. */
   private Created startedPot(UUID a, UUID b, UUID c) throws Exception {
      Created pot = createPot(a);
      assertThat(join(b, pot).status()).isEqualTo(200);
      assertThat(join(c, pot).status()).isEqualTo(200);
      walletPaysEverything(pot.id());
      startTimeComes(pot.id());
      assertThat(pot(a, pot.id()).path("status").asText()).isEqualTo("ACTIVE");
      return pot;
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

   private String statusOf(UUID viewer, UUID pot, UUID member) throws Exception {
      for (JsonNode m : pot(viewer, pot).path("members")) {
         if (m.path("userId").asText().equals(member.toString())) {
            return m.path("status").asText();
         }
      }
      return "NOT A MEMBER";
   }

   // ------------------------------------------------------------------ creating and joining

   @Test
   void creatingAPotAnnouncesItAsksTheOwnerToPayAndShowsTheInviteCodeToTheOwnerOnly() throws Exception {
      UUID owner = UUID.randomUUID();
      Created pot = createPot(owner);

      JsonNode created = pot(owner, pot.id());
      assertThat(created.path("status").asText()).isEqualTo("OPEN");
      assertThat(created.path("inviteCode").asText()).isEqualTo(pot.code()).hasSize(16);
      assertThat(pot(UUID.randomUUID(), pot.id()).path("inviteCode").isNull()).isTrue();
      assertThat(jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ? and payload like '%created%'",
            Integer.class, pot.id().toString())).isEqualTo(1);

      List<JsonNode> requests = pending(pot.id(), "CONTRIBUTION");
      assertThat(requests).hasSize(1);
      assertThat(requests.get(0).path("userId").asText()).isEqualTo(owner.toString());
      assertThat(requests.get(0).path("amount").asLong()).isEqualTo(CONTRIBUTION);
      assertThat(statusOf(owner, pot.id(), owner)).isEqualTo("PENDING");

      answer(requests.get(0), true);
      assertThat(statusOf(owner, pot.id(), owner)).isEqualTo("ACTIVE");
   }

   @Test
   void aPotCannotStartInThePastOrNeedsAStartTime() throws Exception {
      UUID owner = UUID.randomUUID();
      String past = Instant.now().minusSeconds(60).toString();

      assertThat(as(owner, "POST", "/api/pots", "{\"name\":\"x\",\"contribution\":100000,\"startsAt\":\"" + past + "\"}").status()).isEqualTo(422);
      assertThat(as(owner, "POST", "/api/pots", "{\"name\":\"x\",\"contribution\":100000}").status()).isEqualTo(400);
   }

   @Test
   void joiningNeedsTheInvitationExceptForTheOwner() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID guest = UUID.randomUUID();
      Created pot = createPot(owner);

      assertThat(as(guest, "POST", "/api/pots/" + pot.id() + "/join", null).status()).isEqualTo(403);
      assertThat(as(guest, "POST", "/api/pots/" + pot.id() + "/join", "{\"inviteCode\":\"wrong\"}").status()).isEqualTo(403);
      assertThat(join(guest, pot).status()).isEqualTo(200);
      assertThat(join(guest, pot).status()).isEqualTo(422); // already in
      assertThat(as(owner, "POST", "/api/pots/" + pot.id() + "/join", null).status()).isEqualTo(422); // the owner is in already
   }

   @Test
   void aMemberIsInOnlyOnceTheirContributionHasArrived() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID guest = UUID.randomUUID();
      Created pot = createPot(owner);

      join(guest, pot);
      assertThat(statusOf(owner, pot.id(), guest)).isEqualTo("PENDING");
      assertThat(pending(pot.id(), "CONTRIBUTION")).hasSize(2);

      walletPaysEverything(pot.id());

      assertThat(statusOf(owner, pot.id(), guest)).isEqualTo("ACTIVE");
      JsonNode open = pot(owner, pot.id());
      assertThat(open.path("memberCount").asInt()).isEqualTo(2);
      assertThat(open.path("potAmount").asLong()).isEqualTo(2 * CONTRIBUTION);
   }

   @Test
   void aRefusedPaymentMeansNotBeingInAndTheMemberCanTryAgain() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID poor = UUID.randomUUID();
      Created pot = createPot(owner);
      join(poor, pot);

      JsonNode request = pending(pot.id(), "CONTRIBUTION").stream().filter(r -> r.path("userId").asText().equals(poor.toString())).findFirst().orElseThrow();
      answer(request, false);

      assertThat(statusOf(owner, pot.id(), poor)).isEqualTo("NOT A MEMBER");
      assertThat(pot(poor, pot.id()).path("joinProblem").asText()).isEqualTo("Insufficient funds");
      assertThat(join(poor, pot).status()).isEqualTo(200); // asks the wallet again
      assertThat(statusOf(owner, pot.id(), poor)).isEqualTo("PENDING");
   }

   // ------------------------------------------------------------------ leaving

   @Test
   void aMemberCanLeaveBeforeBiddingStartsAndGetsTheirMoneyBack() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID guest = UUID.randomUUID();
      Created pot = createPot(owner);
      join(guest, pot);
      walletPaysEverything(pot.id());

      assertThat(as(guest, "POST", "/api/pots/" + pot.id() + "/leave", null).status()).isEqualTo(200);

      List<JsonNode> refunds = pending(pot.id(), "REFUND");
      assertThat(refunds).hasSize(1);
      assertThat(refunds.get(0).path("userId").asText()).isEqualTo(guest.toString());
      assertThat(refunds.get(0).path("amount").asLong()).isEqualTo(CONTRIBUTION);
      assertThat(statusOf(owner, pot.id(), guest)).isEqualTo("NOT A MEMBER");
      assertThat(pot(owner, pot.id()).path("memberCount").asInt()).isEqualTo(1);

      assertThat(as(guest, "POST", "/api/pots/" + pot.id() + "/leave", null).status()).isEqualTo(422); // no longer in
      assertThat(join(guest, pot).status()).isEqualTo(200); // and can come back
   }

   @Test
   void theOwnerAndAMemberWhoseMoneyIsStillOnItsWayCannotLeave() throws Exception {
      UUID owner = UUID.randomUUID();
      UUID guest = UUID.randomUUID();
      Created pot = createPot(owner);
      join(guest, pot);

      assertThat(as(owner, "POST", "/api/pots/" + pot.id() + "/leave", null).status()).isEqualTo(422);
      assertThat(as(guest, "POST", "/api/pots/" + pot.id() + "/leave", null).status()).isEqualTo(422); // still pending
   }

   @Test
   void onceBiddingHasStartedNobodyCanLeaveOrJoin() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      Created pot = startedPot(a, b, UUID.randomUUID());

      assertThat(as(b, "POST", "/api/pots/" + pot.id() + "/leave", null).status()).isEqualTo(422);
      assertThat(join(UUID.randomUUID(), pot).status()).isEqualTo(422);
   }

   // ------------------------------------------------------------------ the start time

   @Test
   void atTheStartTimeTheGroupIsFixedAndBiddingOpensAtOnce() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID c = UUID.randomUUID();
      Created pot = startedPot(a, b, c);

      JsonNode started = pot(a, pot.id());
      assertThat(started.path("memberCount").asInt()).isEqualTo(3);
      assertThat(started.path("potAmount").asLong()).isEqualTo(POT);
      assertThat(started.path("currentRound").asInt()).isEqualTo(1);
      assertThat(started.path("round").path("status").asText()).isEqualTo("BIDDING");
      assertThat(started.path("round").path("biddingEndsAt").asText()).isNotBlank();
   }

   @Test
   void aPotThatIsNotYetDueIsLeftAlone() throws Exception {
      UUID a = UUID.randomUUID();
      Created pot = createPot(a);
      join(UUID.randomUUID(), pot);
      walletPaysEverything(pot.id());

      startScheduler.startPotsWhoseTimeHasCome();

      assertThat(pot(a, pot.id()).path("status").asText()).isEqualTo("OPEN");
   }

   @Test
   void aPotWithFewerThanTwoMembersIsCancelledAndTheOwnerIsRefunded() throws Exception {
      UUID owner = UUID.randomUUID();
      Created pot = createPot(owner);
      walletPaysEverything(pot.id());

      startTimeComes(pot.id());

      assertThat(pot(owner, pot.id()).path("status").asText()).isEqualTo("CANCELLED");
      List<JsonNode> refunds = pending(pot.id(), "REFUND");
      assertThat(refunds).hasSize(1);
      assertThat(refunds.get(0).path("userId").asText()).isEqualTo(owner.toString());
   }

   @Test
   void aMemberWhosePaymentWasStillOnItsWayAtTheStartIsOutAndRefundedWhenItArrives() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID late = UUID.randomUUID();
      Created pot = createPot(a);
      join(b, pot);
      join(late, pot);
      List<JsonNode> requests = pending(pot.id(), "CONTRIBUTION");
      JsonNode lates = requests.stream().filter(r -> r.path("userId").asText().equals(late.toString())).findFirst().orElseThrow();
      requests.stream().filter(r -> r != lates).forEach(r -> answer(r, true));

      startTimeComes(pot.id());

      assertThat(pot(a, pot.id()).path("memberCount").asInt()).isEqualTo(2);
      assertThat(statusOf(a, pot.id(), late)).isEqualTo("NOT A MEMBER");
      answer(lates, true); // the money arrives after the start
      List<JsonNode> refunds = pending(pot.id(), "REFUND");
      assertThat(refunds).hasSize(1);
      assertThat(refunds.get(0).path("userId").asText()).isEqualTo(late.toString());
   }

   // ------------------------------------------------------------------ bidding

   @Test
   void bidsFollowTheRulesOfTheRound() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID outsider = UUID.randomUUID();
      Created pot = startedPot(a, b, UUID.randomUUID());

      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":179999}").status()).isEqualTo(422); // below 60%
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":300001}").status()).isEqualTo(422); // above the pot
      assertThat(as(outsider, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}").status()).isEqualTo(403);
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}").status()).isEqualTo(200);
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}").status()).isEqualTo(422); // not lower
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":260000}").status()).isEqualTo(422); // higher
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":240000}").status()).isEqualTo(200); // lowered

      JsonNode seen = pot(a, pot.id()).path("round").path("bids");
      assertThat(seen).hasSize(1);
      assertThat(seen.get(0).path("amount").asLong()).isEqualTo(240_000);
      assertThat(pot(outsider, pot.id()).path("round").path("bids")).isEmpty(); // bids are for members only
   }

   @Test
   void aMemberWhoLeftCannotBid() throws Exception {
      UUID a = UUID.randomUUID();
      UUID leaver = UUID.randomUUID();
      Created pot = createPot(a);
      join(leaver, pot);
      join(UUID.randomUUID(), pot);
      join(UUID.randomUUID(), pot);
      walletPaysEverything(pot.id());
      as(leaver, "POST", "/api/pots/" + pot.id() + "/leave", null);
      startTimeComes(pot.id());

      assertThat(as(leaver, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}").status()).isEqualTo(403);
   }

   // ------------------------------------------------------------------ settling

   @Test
   void theLowestBidWinsAndTheRestGoesBackToTheOthersAsDividends() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID c = UUID.randomUUID();
      Created pot = startedPot(a, b, c);
      as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}");
      as(b, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":240000}");
      as(c, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":270000}");

      assertThat(as(c, "POST", "/api/pots/" + pot.id() + "/settle", null).status()).isEqualTo(403); // not the owner
      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/settle", null).status()).isEqualTo(200);

      // b wins Rs 2,400; the Rs 600 left is shared by a and c: Rs 300 each
      List<JsonNode> requests = pendingRequests(pot.id());
      assertThat(requests).hasSize(3);
      JsonNode payout = requests.stream().filter(r -> r.path("kind").asText().equals("PAYOUT")).findFirst().orElseThrow();
      assertThat(payout.path("userId").asText()).isEqualTo(b.toString());
      assertThat(payout.path("amount").asLong()).isEqualTo(240_000);
      assertThat(requests.stream().filter(r -> r.path("kind").asText().equals("DIVIDEND")).map(r -> r.path("amount").asLong()))
            .containsExactly(30_000L, 30_000L);
      assertThat(pot(a, pot.id()).path("round").path("status").asText()).isEqualTo("SETTLING");
   }

   @Test
   void theSchedulerSettlesARoundWhoseBiddingTimeIsOver() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      Created pot = startedPot(a, b, UUID.randomUUID());
      as(b, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":200000}");

      settlementScheduler.settleRoundsWhoseBiddingEnded();
      assertThat(pot(a, pot.id()).path("round").path("status").asText()).isEqualTo("BIDDING"); // time is not over

      jdbc.update("update pot_rounds set bidding_ends_at = ? where pot_id = ?", Timestamp.from(Instant.now().minusSeconds(5)), pot.id());
      settlementScheduler.settleRoundsWhoseBiddingEnded();

      JsonNode round = pot(a, pot.id()).path("round");
      assertThat(round.path("status").asText()).isEqualTo("SETTLING");
      assertThat(round.path("winnerId").asText()).isEqualTo(b.toString());
   }

   @Test
   void aRejectedContributionInALaterRoundCanBePaidAgainAndKeepsItsReference() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      Created pot = startedPot(a, b, UUID.randomUUID());
      as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}");
      as(a, "POST", "/api/pots/" + pot.id() + "/settle", null);
      walletPaysEverything(pot.id()); // round 1 is paid out; round 2 asks everyone for their contribution

      List<JsonNode> requests = pending(pot.id(), "CONTRIBUTION");
      assertThat(requests).hasSize(3);
      JsonNode poor = requests.stream().filter(r -> r.path("userId").asText().equals(b.toString())).findFirst().orElseThrow();
      requests.stream().filter(r -> r != poor).forEach(r -> answer(r, true));
      answer(poor, false);
      assertThat(pot(a, pot.id()).path("round").path("status").asText()).isEqualTo("COLLECTING");

      assertThat(as(a, "POST", "/api/pots/" + pot.id() + "/contribution", null).status()).isEqualTo(422); // a already paid
      answered.remove(poor.path("reference").asText()); // the member asks again, so the wallet is waiting again
      assertThat(as(b, "POST", "/api/pots/" + pot.id() + "/contribution", null).status()).isEqualTo(200);
      List<JsonNode> again = pendingRequests(pot.id());
      assertThat(again).hasSize(1);
      assertThat(again.get(0).path("reference").asText()).isEqualTo(poor.path("reference").asText());

      answer(again.get(0), true);
      assertThat(pot(a, pot.id()).path("round").path("status").asText()).isEqualTo("BIDDING");
   }

   @Test
   void aWholePotRunsToTheEndAndEveryMemberWinsExactlyOnce() throws Exception {
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID c = UUID.randomUUID();
      Created pot = startedPot(a, b, c);

      // round 1: b wins with the lowest bid
      as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":250000}");
      as(b, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":240000}");
      as(a, "POST", "/api/pots/" + pot.id() + "/settle", null);
      walletPaysEverything(pot.id());

      JsonNode afterRound1 = pot(a, pot.id());
      assertThat(afterRound1.path("currentRound").asInt()).isEqualTo(2);
      assertThat(afterRound1.path("round").path("status").asText()).isEqualTo("COLLECTING");
      assertThat(afterRound1.path("history").get(0).path("winnerId").asText()).isEqualTo(b.toString());

      // round 2: b has won, so b cannot bid; c wins against a
      walletPaysEverything(pot.id());
      assertThat(pot(a, pot.id()).path("round").path("status").asText()).isEqualTo("BIDDING");
      assertThat(as(b, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":200000}").status()).isEqualTo(422);
      as(a, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":280000}");
      as(c, "POST", "/api/pots/" + pot.id() + "/bids", "{\"amount\":190000}");
      as(a, "POST", "/api/pots/" + pot.id() + "/settle", null);
      walletPaysEverything(pot.id());

      // round 3: only a is left, so once the contributions are in, a is paid the whole pot with no bidding
      walletPaysEverything(pot.id());
      JsonNode last = pot(a, pot.id());
      assertThat(last.path("currentRound").asInt()).isEqualTo(3);
      assertThat(last.path("round").path("status").asText()).isEqualTo("SETTLING");
      assertThat(last.path("round").path("winnerId").asText()).isEqualTo(a.toString());
      assertThat(last.path("round").path("payout").asLong()).isEqualTo(POT);
      walletPaysEverything(pot.id());

      JsonNode finished = pot(a, pot.id());
      assertThat(finished.path("status").asText()).isEqualTo("COMPLETED");
      assertThat(finished.path("members")).allSatisfy(member -> assertThat(member.path("won").asBoolean()).isTrue());
      assertThat(finished.path("history")).hasSize(3);

      // the money adds up: contributions in = payouts + dividends out
      assertThat(sumOf(pot.id(), "CONTRIBUTION")).isEqualTo(3 * POT);
      assertThat(sumOf(pot.id(), "PAYOUT") + sumOf(pot.id(), "DIVIDEND")).isEqualTo(3 * POT);
   }

   @Test
   void aRepeatedAnswerFromTheWalletChangesNothing() throws Exception {
      UUID a = UUID.randomUUID();
      Created pot = createPot(a);
      join(UUID.randomUUID(), pot);
      List<JsonNode> requests = pendingRequests(pot.id());
      requests.forEach(request -> answer(request, true));
      int before = jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ?", Integer.class, pot.id().toString());

      requests.forEach(request -> answer(request, true));

      assertThat(pot(a, pot.id()).path("memberCount").asInt()).isEqualTo(2);
      assertThat(jdbc.queryForObject("select count(*) from outbox_events where aggregate_type = 'pot' and aggregate_id = ?", Integer.class, pot.id().toString())).isEqualTo(before);
   }

   @Test
   void potsAreListedOnlyForTheirMembers() throws Exception {
      UUID owner = UUID.randomUUID();
      Created pot = createPot(owner);

      JsonNode mine = as(owner, "GET", "/api/pots", null).body().path("items");
      JsonNode notMine = as(UUID.randomUUID(), "GET", "/api/pots", null).body().path("items");

      assertThat(mine).hasSize(1);
      assertThat(mine.get(0).path("id").asText()).isEqualTo(pot.id().toString());
      assertThat(notMine).isEmpty();
   }

   @Test
   void everythingNeedsASignedInMember() throws Exception {
      assertThat(call("GET", "/api/pots", null, null).status()).isEqualTo(401);
      assertThat(call("POST", "/api/pots", "{}", null).status()).isEqualTo(401);
   }

   // ------------------------------------------------------------------ live updates

   /** Opens the live connection of a pot with a ticket; what arrives is queued, and a close is reported. */
   private record Watch(BlockingQueue<String> messages, CompletableFuture<Integer> closed) {
   }

   private Watch watch(String ticket) throws Exception {
      BlockingQueue<String> messages = new LinkedBlockingQueue<>();
      CompletableFuture<Integer> closed = new CompletableFuture<>();
      http.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + port + "/api/ws/pots?ticket=" + ticket), new WebSocket.Listener() {
         @Override
         public void onOpen(WebSocket socket) {
            socket.request(1);
         }

         @Override
         public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            messages.add(data.toString());
            socket.request(1);
            return null;
         }

         @Override
         public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
         }
      }).get(5, TimeUnit.SECONDS);
      return new Watch(messages, closed);
   }

   private String ticket(UUID user, UUID pot) throws Exception {
      Reply reply = as(user, "POST", "/api/pots/" + pot + "/socket-ticket", null);
      assertThat(reply.status()).isEqualTo(200);
      return reply.body().path("ticket").asText();
   }

   @Test
   void aMemberWatchingAPotIsToldWhenSomeoneJoins() throws Exception {
      UUID owner = UUID.randomUUID();
      Created pot = createPot(owner);
      Watch watching = watch(ticket(owner, pot.id()));

      join(UUID.randomUUID(), pot);

      assertThat(watching.messages().poll(5, TimeUnit.SECONDS)).isEqualTo("changed");
   }

   @Test
   void aWatcherOfOnePotHearsNothingAboutAnother() throws Exception {
      UUID owner = UUID.randomUUID();
      Created watched = createPot(owner);
      Created other = createPot(UUID.randomUUID());
      Watch watching = watch(ticket(owner, watched.id()));

      join(UUID.randomUUID(), other);

      assertThat(watching.messages().poll(1, TimeUnit.SECONDS)).isNull();
   }

   @Test
   void onlyMembersGetATicketAndATicketOpensOneConnectionOnly() throws Exception {
      UUID owner = UUID.randomUUID();
      Created pot = createPot(owner);

      assertThat(as(UUID.randomUUID(), "POST", "/api/pots/" + pot.id() + "/socket-ticket", null).status()).isEqualTo(403);
      assertThat(call("POST", "/api/pots/" + pot.id() + "/socket-ticket", null, null).status()).isEqualTo(401);

      String ticket = ticket(owner, pot.id());
      Watch first = watch(ticket);
      Watch second = watch(ticket);
      Watch invented = watch("not-a-ticket");

      // the two connections race for the ticket: one gets in, the other is closed
      assertThat(CompletableFuture.anyOf(first.closed(), second.closed()).get(5, TimeUnit.SECONDS)).isEqualTo(1008);
      Thread.sleep(300);
      assertThat(List.of(first, second).stream().filter(watch -> watch.closed().isDone()).count()).isEqualTo(1);
      assertThat(invented.closed().get(5, TimeUnit.SECONDS)).isEqualTo(1008);
   }
}
