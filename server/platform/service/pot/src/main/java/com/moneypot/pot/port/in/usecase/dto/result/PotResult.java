package com.moneypot.pot.port.in.usecase.dto.result;

import com.moneypot.pot.domain.entity.MemberStatus;
import com.moneypot.pot.domain.entity.PotStatus;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.entity.TransferStatus;
import com.moneypot.shared.dto.result.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A pot with its members and its running round. Money is in paise.
 *
 * @param memberCount the members who have paid (before bidding starts), or the group that started
 * @param potAmount   what a round pays out: every member contributes once
 * @param startsAt    when joining closes and bidding begins
 * @param inviteCode  what a member needs to join; only the owner gets it, others see null
 * @param joinProblem why the viewer is not in, when their payment was refused (for example "Insufficient funds"); else null
 * @param round       null until bidding starts
 * @param history     the rounds that are over, with who won and what they were paid
 */
public record PotResult(
      UUID id,
      String name,
      UUID ownerId,
      PotStatus status,
      int memberCount,
      long contribution,
      long potAmount,
      long minBid,
      int minBidPercent,
      int bidWindowSeconds,
      int currentRound,
      Instant startsAt,
      String inviteCode,
      String joinProblem,
      List<Member> members,
      RoundView round,
      List<Past> history
) implements Result.One {

   public PotResult {
      members = members == null ? List.of() : List.copyOf(members);
      history = history == null ? List.of() : List.copyOf(history);
   }

   public record Member(UUID userId, int joinOrder, MemberStatus status, boolean won, Integer wonRound) {
   }

   /**
    * @param winnerId set once the winner is decided
    * @param payout   what the winner receives
    * @param bids     lowest first; empty unless the viewer is a member
    */
   public record RoundView(
         UUID id,
         int number,
         RoundStatus status,
         Instant biddingEndsAt,
         UUID winnerId,
         Long payout,
         List<Transfer> transfers,
         List<BidView> bids
   ) {
      public RoundView {
         transfers = transfers == null ? List.of() : List.copyOf(transfers);
         bids = bids == null ? List.of() : List.copyOf(bids);
      }
   }

   public record Transfer(UUID userId, TransferKind kind, long amount, TransferStatus status, String reason) {
   }

   public record Past(int number, UUID winnerId, Long payout) {
   }

   public record BidView(UUID userId, long amount, Instant placedAt) {
   }
}
