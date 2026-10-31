package com.moneypot.pot.adapter.in.web.controller.dto.response;

import com.moneypot.pot.domain.entity.MemberStatus;
import com.moneypot.pot.domain.entity.PotStatus;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.entity.TransferStatus;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A pot with its members and the running round. Money is in paise. {@code inviteCode} is only set for the owner, and
 * {@code joinProblem} only for someone who tried to join and was refused.
 */
public record PotResponse(
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
      boolean demo,
      List<Member> members,
      Round round,
      List<Past> history
) {

   public record Member(UUID userId, int joinOrder, MemberStatus status, boolean won, Integer wonRound) {
   }

   public record Round(UUID id, int number, RoundStatus status, Instant biddingEndsAt, UUID winnerId, Long payout,
                       List<Transfer> transfers, List<Bid> bids) {
   }

   public record Transfer(UUID userId, TransferKind kind, long amount, TransferStatus status, String reason) {
   }

   public record Bid(UUID userId, long amount, Instant placedAt) {
   }

   public record Past(int number, UUID winnerId, Long payout) {
   }

   public static PotResponse from(PotResult pot) {
      PotResult.RoundView round = pot.round();
      return new PotResponse(
            pot.id(), pot.name(), pot.ownerId(), pot.status(), pot.memberCount(), pot.contribution(), pot.potAmount(),
            pot.minBid(), pot.minBidPercent(), pot.bidWindowSeconds(), pot.currentRound(), pot.startsAt(),
            pot.inviteCode(), pot.joinProblem(), pot.demo(),
            pot.members().stream().map(m -> new Member(m.userId(), m.joinOrder(), m.status(), m.won(), m.wonRound())).toList(),
            round == null ? null : new Round(round.id(), round.number(), round.status(), round.biddingEndsAt(),
                  round.winnerId(), round.payout(),
                  round.transfers().stream().map(t -> new Transfer(t.userId(), t.kind(), t.amount(), t.status(), t.reason())).toList(),
                  round.bids().stream().map(b -> new Bid(b.userId(), b.amount(), b.placedAt())).toList()),
            pot.history().stream().map(h -> new Past(h.number(), h.winnerId(), h.payout())).toList());
   }
}
