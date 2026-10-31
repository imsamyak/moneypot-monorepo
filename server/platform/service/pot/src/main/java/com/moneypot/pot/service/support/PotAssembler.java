package com.moneypot.pot.service.support;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.entity.TransferStatus;
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
 * the transfers of the round are only shown to the members of the pot, and only the owner sees the invite code.
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
      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId()).stream().filter(m -> !m.isLeft()).toList();
      boolean viewerIsMember = viewerId != null && members.stream().anyMatch(m -> m.getUserId().equals(viewerId));

      // before bidding starts the group is whoever has paid so far; afterwards it is fixed
      int memberCount = pot.isOpen() ? (int) members.stream().filter(PotMember::isActive).count() : pot.getMemberCount();

      PotResult.RoundView round = null;
      if (pot.getCurrentRound() > 0) {
         Round current = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound()).orElse(null);
         if (current != null) {
            round = roundView(current, viewerIsMember);
         }
      }
      List<PotResult.Past> history = loadRoundPort.loadByPotId(pot.getId()).stream()
            .filter(r -> r.getStatus() == RoundStatus.SETTLED)
            .map(r -> new PotResult.Past(r.getNumber(), r.getWinnerId(), r.getPayout()))
            .toList();

      return new PotResult(
            pot.getId(), pot.getName(), pot.getOwnerId(), pot.getStatus(), memberCount, pot.getContribution(),
            pot.potAmountFor(memberCount), pot.minBidFor(memberCount), pot.getMinBidPercent(), pot.getBidWindowSeconds(),
            pot.getCurrentRound(), pot.getStartsAt(),
            viewerId != null && viewerId.equals(pot.getOwnerId()) ? pot.getInviteCode() : null,
            viewerId != null && !viewerIsMember ? joinProblem(pot, viewerId) : null,
            pot.isDemo(),
            members.stream().map(m -> new PotResult.Member(m.getUserId(), m.getJoinOrder(), m.getStatus(), m.isWon(), m.getWonRound())).toList(),
            round, history);
   }

   /** Why the viewer's last attempt to join did not work (the wallet refused the payment), or null. */
   private String joinProblem(Pot pot, UUID viewerId) {
      if (!pot.isOpen()) {
         return null;
      }
      return loadRoundPort.loadByPotIdAndNumber(pot.getId(), 1)
            .flatMap(round -> loadTransferPort.loadByRoundId(round.getId()).stream()
                  .filter(t -> t.getUserId().equals(viewerId) && t.getKind() == TransferKind.CONTRIBUTION
                        && t.getStatus() == TransferStatus.VOID && t.getReason() != null && !t.getReason().equals(REFUNDED))
                  .reduce((first, second) -> second))
            .map(RoundTransfer::getReason)
            .orElse(null);
   }

   /** The reason kept on a contribution that was given back, as opposed to one the wallet refused. */
   public static final String REFUNDED = "refunded";

   private PotResult.RoundView roundView(Round round, boolean showDetails) {
      List<PotResult.Transfer> transfers = List.of();
      List<PotResult.BidView> bids = List.of();
      if (showDetails) {
         transfers = loadTransferPort.loadByRoundId(round.getId()).stream()
               .filter(t -> !t.isVoid())
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
