package com.moneypot.pot.domain.entity;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PotTest {

   private final UUID owner = UUID.randomUUID();
   private final Instant now = Instant.parse("2026-09-20T10:00:00Z");
   private final Instant startsAt = now.plus(Duration.ofDays(3));

   private Pot pot() {
      return Pot.create(owner, "Friends", 500_000, 60, 3_600, startsAt, now);
   }

   @Test
   void aNewPotIsOpenWithNoGroupYetAndAnInviteCode() {
      Pot pot = pot();

      assertThat(pot.getStatus()).isEqualTo(PotStatus.OPEN);
      assertThat(pot.getMemberCount()).isZero();
      assertThat(pot.getCurrentRound()).isZero();
      assertThat(pot.getInviteCode()).hasSize(16).isNotEqualTo(pot().getInviteCode());
   }

   @Test
   void thePotIsEveryMembersContributionAndTheLowestBidIsAShareOfIt() {
      Pot pot = pot();

      assertThat(pot.potAmountFor(10)).isEqualTo(5_000_000);
      assertThat(pot.minBidFor(10)).isEqualTo(3_000_000);
   }

   @Test
   void theLowestBidIsRoundedUpSoItNeverFallsBelowThePercentageAsked() {
      Pot pot = Pot.create(owner, "Odd", 333, 50, 60, startsAt, now);

      assertThat(pot.potAmountFor(3)).isEqualTo(999);
      assertThat(pot.minBidFor(3)).isEqualTo(500);
   }

   @Test
   void joiningIsOpenOnlyUntilTheBiddingStartTime() {
      Pot pot = pot();

      assertThat(pot.joiningIsOpen(startsAt.minusSeconds(1))).isTrue();
      assertThat(pot.joiningIsOpen(startsAt)).isFalse();
      assertThat(pot.startTimeHasCome(startsAt)).isTrue();
   }

   @Test
   void startingFixesTheGroupClosesJoiningAndBeginsRoundOne() {
      Pot pot = pot();

      pot.start(4);

      assertThat(pot.getStatus()).isEqualTo(PotStatus.ACTIVE);
      assertThat(pot.getMemberCount()).isEqualTo(4);
      assertThat(pot.getCurrentRound()).isEqualTo(1);
      assertThat(pot.potAmount()).isEqualTo(2_000_000);
      assertThat(pot.joiningIsOpen(now)).isFalse();
      assertThatThrownBy(() -> pot.start(4)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aPotCannotStartWithFewerThanTwoMembers() {
      assertThatThrownBy(() -> pot().start(1)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void onlyAnOpenPotCanBeCancelled() {
      Pot cancelled = pot();
      cancelled.cancel();
      Pot running = pot();
      running.start(2);

      assertThat(cancelled.getStatus()).isEqualTo(PotStatus.CANCELLED);
      assertThatThrownBy(running::cancel).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aPotCannotMoveToTheNextRoundBeforeItStarts() {
      assertThatThrownBy(() -> pot().nextRound()).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void theLimitsAreChecked() {
      assertThatThrownBy(() -> Pot.create(owner, " ", 500_000, 60, 3_600, startsAt, now)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 99, 60, 3_600, startsAt, now)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 500_000, 0, 3_600, startsAt, now)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 500_000, 60, 5, startsAt, now)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void theBiddingStartTimeMustBeInTheFutureButNotTooFarAway() {
      assertThatThrownBy(() -> Pot.create(owner, "x", 500_000, 60, 3_600, now, now)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 500_000, 60, 3_600, now.minusSeconds(60), now)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 500_000, 60, 3_600, now.plus(Duration.ofDays(91)), now)).isInstanceOf(BusinessRuleException.class);
   }
}
