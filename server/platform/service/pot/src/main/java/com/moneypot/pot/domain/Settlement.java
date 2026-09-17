package com.moneypot.pot.domain;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.shared.exception.domain.BusinessRuleException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Who wins a round and how the pot is divided. It is plain code with no database in it, so the rule is easy to read
 * and to test:
 * <ul>
 *   <li>Only members who have not won yet can win. The lowest bid wins; at the same amount the earlier bid wins.</li>
 *   <li>If nobody bid, or only one member is left, the next member in join order wins the whole pot.</li>
 *   <li>The winner gets the winning bid. What is left (pot minus bid) goes to every other member in equal shares as
 *       a dividend; the few paise that do not divide evenly go one each to the first members in join order.</li>
 * </ul>
 * Payout plus dividends always add up to the pot, so the pot account is empty again after the round.
 *
 * @param payout    what the winner receives
 * @param dividends the share of each member who gets one (the winner never does)
 */
public record Settlement(UUID winnerId, long payout, Map<UUID, Long> dividends) {

   public static Settlement decide(long potAmount, List<PotMember> members, List<Bid> bids) {
      List<PotMember> eligible = members.stream()
            .filter(member -> !member.isWon())
            .sorted(Comparator.comparingInt(PotMember::getJoinOrder))
            .toList();
      if (eligible.isEmpty()) {
         throw BusinessRuleException.of("Every member has already won");
      }

      Set<UUID> eligibleIds = eligible.stream().map(PotMember::getUserId).collect(Collectors.toSet());
      Bid best = bids.stream()
            .filter(bid -> eligibleIds.contains(bid.getUserId()))
            .min(Comparator.comparingLong(Bid::getAmount).thenComparing(Bid::getPlacedAt))
            .orElse(null);

      UUID winner;
      long payout;
      if (eligible.size() == 1 || best == null) {
         winner = eligible.get(0).getUserId();
         payout = potAmount;
      } else {
         winner = best.getUserId();
         payout = best.getAmount();
      }

      List<PotMember> others = members.stream()
            .filter(member -> !member.getUserId().equals(winner))
            .sorted(Comparator.comparingInt(PotMember::getJoinOrder))
            .toList();
      long leftover = potAmount - payout;
      Map<UUID, Long> dividends = new LinkedHashMap<>();
      if (leftover > 0 && !others.isEmpty()) {
         long share = leftover / others.size();
         long extra = leftover % others.size();
         for (int i = 0; i < others.size(); i++) {
            long amount = share + (i < extra ? 1 : 0);
            if (amount > 0) {
               dividends.put(others.get(i).getUserId(), amount);
            }
         }
      }
      return new Settlement(winner, payout, dividends);
   }
}
