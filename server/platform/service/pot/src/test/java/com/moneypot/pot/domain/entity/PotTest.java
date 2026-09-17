package com.moneypot.pot.domain.entity;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PotTest {

   private final UUID owner = UUID.randomUUID();

   private Pot pot() {
      return Pot.create(owner, "Friends", 10, 500_000, 60, 3_600);
   }

   @Test
   void thePotIsEveryMembersContributionAndTheLowestBidIsAShareOfIt() {
      Pot pot = pot();

      assertThat(pot.potAmount()).isEqualTo(5_000_000);
      assertThat(pot.minBid()).isEqualTo(3_000_000);
      assertThat(pot.getStatus()).isEqualTo(PotStatus.OPEN);
      assertThat(pot.getCurrentRound()).isZero();
   }

   @Test
   void theLowestBidIsRoundedUpSoItNeverFallsBelowThePercentageAsked() {
      Pot pot = Pot.create(owner, "Odd", 3, 333, 50, 60);

      assertThat(pot.potAmount()).isEqualTo(999);
      assertThat(pot.minBid()).isEqualTo(500);
   }

   @Test
   void startingClosesJoiningAndBeginsRoundOne() {
      Pot pot = pot();

      pot.start();

      assertThat(pot.getStatus()).isEqualTo(PotStatus.ACTIVE);
      assertThat(pot.getCurrentRound()).isEqualTo(1);
      assertThatThrownBy(pot::start).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aPotCannotMoveToTheNextRoundBeforeItStarts() {
      assertThatThrownBy(() -> pot().nextRound()).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void theLimitsAreChecked() {
      assertThatThrownBy(() -> Pot.create(owner, " ", 10, 500_000, 60, 3_600)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 1, 500_000, 60, 3_600)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 51, 500_000, 60, 3_600)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 10, 99, 60, 3_600)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 10, 500_000, 0, 3_600)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Pot.create(owner, "x", 10, 500_000, 60, 5)).isInstanceOf(BusinessRuleException.class);
   }
}
