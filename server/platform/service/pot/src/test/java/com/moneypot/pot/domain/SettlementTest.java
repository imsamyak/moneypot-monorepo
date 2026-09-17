package com.moneypot.pot.domain;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {

   private static final long POT = 5_000_000L; // 10 members x Rs 5,000
   private final Instant t0 = Instant.parse("2026-09-20T10:00:00Z");
   private final UUID pot = UUID.randomUUID();

   private List<PotMember> members(int count) {
      List<PotMember> members = new ArrayList<>();
      for (int order = 1; order <= count; order++) {
         members.add(PotMember.join(pot, UUID.randomUUID(), order));
      }
      return members;
   }

   private Bid bid(PotMember member, long amount, int secondsAfterStart) {
      return Bid.place(UUID.randomUUID(), member.getUserId(), amount, t0.plusSeconds(secondsAfterStart));
   }

   @Test
   void theLowestBidWinsAndIsPaidExactlyWhatItAsked() {
      List<PotMember> members = members(10);
      List<Bid> bids = List.of(bid(members.get(0), 4_300_000, 1), bid(members.get(1), 4_100_000, 2), bid(members.get(2), 4_500_000, 3));

      Settlement settlement = Settlement.decide(POT, members, bids);

      assertThat(settlement.winnerId()).isEqualTo(members.get(1).getUserId());
      assertThat(settlement.payout()).isEqualTo(4_100_000);
   }

   @Test
   void whatTheWinnerLeavesIsSharedEquallyAmongTheOtherMembers() {
      List<PotMember> members = members(10);
      // leftover Rs 9,000 over 9 other members = Rs 1,000 each, exactly the example from the plan
      Settlement settlement = Settlement.decide(POT, members, List.of(bid(members.get(1), 4_100_000, 2)));

      assertThat(settlement.dividends()).hasSize(9).doesNotContainKey(members.get(1).getUserId());
      assertThat(settlement.dividends().values()).containsOnly(100_000L);
   }

   @Test
   void payoutPlusDividendsAlwaysAddUpToThePotEvenWhenItDoesNotDivideEvenly() {
      List<PotMember> members = members(10);
      // a bid that leaves 800,005 paise, which does not divide evenly by the 9 other members
      Settlement settlement = Settlement.decide(POT, members, List.of(bid(members.get(4), 4_199_995, 1)));

      long total = settlement.payout() + settlement.dividends().values().stream().mapToLong(Long::longValue).sum();
      assertThat(total).isEqualTo(POT);
      // 800,005 = 9 x 88,889 + 4: the first four members (in join order, winner excluded) get one extra paisa
      List<Long> shares = settlement.dividends().values().stream().toList();
      assertThat(shares.stream().filter(share -> share == 88_890L).count()).isEqualTo(4);
      assertThat(shares.stream().filter(share -> share == 88_889L).count()).isEqualTo(5);
   }

   @Test
   void atTheSameAmountTheEarlierBidWins() {
      List<PotMember> members = members(5);
      List<Bid> bids = List.of(bid(members.get(3), 3_000_000, 50), bid(members.get(1), 3_000_000, 10), bid(members.get(2), 3_000_000, 30));

      assertThat(Settlement.decide(2_500_000 * 2, members, bids).winnerId()).isEqualTo(members.get(1).getUserId());
   }

   @Test
   void aMemberWhoHasAlreadyWonCannotWinAgainButStillGetsADividend() {
      List<PotMember> members = members(4);
      members.get(0).markWon(1);
      List<Bid> bids = List.of(bid(members.get(0), 1, 1), bid(members.get(2), 3_000, 2));

      Settlement settlement = Settlement.decide(4_000, members, bids);

      assertThat(settlement.winnerId()).isEqualTo(members.get(2).getUserId());
      assertThat(settlement.dividends()).containsKey(members.get(0).getUserId());
   }

   @Test
   void withoutBidsTheNextMemberInJoinOrderWinsTheWholePotAndNoOneGetsADividend() {
      List<PotMember> members = members(4);
      members.get(0).markWon(1);

      Settlement settlement = Settlement.decide(4_000, members, List.of());

      assertThat(settlement.winnerId()).isEqualTo(members.get(1).getUserId());
      assertThat(settlement.payout()).isEqualTo(4_000);
      assertThat(settlement.dividends()).isEmpty();
   }

   @Test
   void theLastMemberLeftWinsTheWholePot() {
      List<PotMember> members = members(3);
      members.get(0).markWon(1);
      members.get(1).markWon(2);

      Settlement settlement = Settlement.decide(3_000, members, List.of(bid(members.get(2), 1_800, 1)));

      assertThat(settlement.winnerId()).isEqualTo(members.get(2).getUserId());
      assertThat(settlement.payout()).isEqualTo(3_000);
      assertThat(settlement.dividends()).isEmpty();
   }

   @Test
   void aBidAtTheWholePotLeavesNothingToShare() {
      List<PotMember> members = members(3);

      Settlement settlement = Settlement.decide(3_000, members, List.of(bid(members.get(1), 3_000, 1)));

      assertThat(settlement.payout()).isEqualTo(3_000);
      assertThat(settlement.dividends()).isEmpty();
   }

   @Test
   void aPotWhereEveryoneHasWonCannotBeSettled() {
      List<PotMember> members = members(2);
      members.forEach(member -> member.markWon(1));

      assertThatThrownBy(() -> Settlement.decide(2_000, members, List.of())).isInstanceOf(BusinessRuleException.class);
   }
}
