package com.moneypot.pot.service.support;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.persistence.LoadBidPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Builds the {@link PotResult} every pot use case answers with: the pot, its members and the running round. The bids and
 * the transfers of the round are only shown to the members of the pot.
 */
@Component
@RequiredArgsConstructor
public class PotAssembler {

   private final LoadMemberPort loadMemberPort;
   private final LoadRoundPort loadRoundPort;
   private final LoadTransferPort loadTransferPort;
   private final LoadBidPort loadBidPort;

   /** @param viewerId who is looking; null for the system */
   public PotResult assemble(Pot pot, UUID viewerId) {
      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      boolean viewerIsMember = viewerId != null && members.stream().anyMatch(m -> m.getUserId().equals(viewerId));

      PotResult.RoundView round = null;
      if (pot.getCurrentRound() > 0) {
         Round current = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound()).orElse(null);
         if (current != null) {
            round = roundView(current, viewerIsMember);
         }
      }
      List<PotResult.Past> history = loadRoundPort.loadByPotId(pot.getId()).stream()
            .filter(r -> r.getWinnerId() != null && r.getNumber() < pot.getCurrentRound())
            .map(r -> new PotResult.Past(r.getNumber(), r.getWinnerId(), r.getPayout()))
            .toList();

      return new PotResult(
            pot.getId(), pot.getName(), pot.getOwnerId(), pot.getStatus(), pot.getMemberCount(), pot.getContribution(),
            pot.potAmount(), pot.minBid(), pot.getMinBidPercent(), pot.getBidWindowSeconds(), pot.getCurrentRound(),
            members.stream().map(m -> new PotResult.Member(m.getUserId(), m.getJoinOrder(), m.isWon(), m.getWonRound())).toList(),
            round, history);
   }

   private PotResult.RoundView roundView(Round round, boolean showDetails) {
      List<PotResult.Transfer> transfers = List.of();
      List<PotResult.BidView> bids = List.of();
      if (showDetails) {
         transfers = loadTransferPort.loadByRoundId(round.getId()).stream()
               .map(PotAssembler::transferView)
               .toList();
         bids = loadBidPort.loadByRoundId(round.getId()).stream()
               .map(PotAssembler::bidView)
               .toList();
      }
      return new PotResult.RoundView(round.getId(), round.getNumber(), round.getStatus(), round.getBiddingEndsAt(),
            round.getWinnerId(), round.getPayout(), transfers, bids);
   }

   private static PotResult.Transfer transferView(RoundTransfer transfer) {
      return new PotResult.Transfer(transfer.getUserId(), transfer.getKind(), transfer.getAmount(), transfer.getStatus(),
            transfer.getReason());
   }

   private static PotResult.BidView bidView(Bid bid) {
      return new PotResult.BidView(bid.getUserId(), bid.getAmount(), bid.getPlacedAt());
   }
}
