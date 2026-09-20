package com.moneypot.pot.service.support;

import com.moneypot.pot.domain.Settlement;
import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult.TransferRequest;
import com.moneypot.pot.port.out.persistence.LoadBidPort;
import com.moneypot.pot.port.out.persistence.SaveRoundPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The two things that start a stretch of the round, shared by the use cases that need them: opening a round (ask every
 * member for their contribution) and settling it (decide the winner, then ask the pot to pay the winner and the other
 * members). Each returns the money movements to request from the wallet.
 */
@Component
@RequiredArgsConstructor
public class RoundSupport {

   private final SaveRoundPort saveRoundPort;
   private final SaveTransferPort saveTransferPort;
   private final LoadBidPort loadBidPort;

   /** Opens the pot's current round and asks every member for their contribution. */
   public List<TransferRequest> openRound(Pot pot, List<PotMember> members) {
      Round round = saveRoundPort.save(Round.open(pot.getId(), pot.getCurrentRound()));
      List<RoundTransfer> transfers = members.stream()
            .map(member -> RoundTransfer.request(pot.getId(), round.getId(), member.getUserId(),
                  TransferKind.CONTRIBUTION, pot.getContribution()))
            .toList();
      saveTransferPort.saveAll(transfers);
      return transfers.stream().map(RoundSupport::toRequest).toList();
   }

   /** Decides the winner of the round and asks the pot to pay the winner and share the rest. */
   public List<TransferRequest> settle(Pot pot, Round round, List<PotMember> members) {
      List<Bid> bids = loadBidPort.loadByRoundId(round.getId());
      Settlement settlement = Settlement.decide(pot.potAmount(), members, bids);

      round.startSettling(settlement.winnerId(), settlement.payout());
      saveRoundPort.save(round);

      List<RoundTransfer> transfers = new ArrayList<>();
      transfers.add(RoundTransfer.request(pot.getId(), round.getId(), settlement.winnerId(), TransferKind.PAYOUT,
            settlement.payout()));
      for (Map.Entry<java.util.UUID, Long> dividend : settlement.dividends().entrySet()) {
         transfers.add(RoundTransfer.request(pot.getId(), round.getId(), dividend.getKey(), TransferKind.DIVIDEND,
               dividend.getValue()));
      }
      saveTransferPort.saveAll(transfers);
      return transfers.stream().map(RoundSupport::toRequest).toList();
   }

   private static TransferRequest toRequest(RoundTransfer transfer) {
      return new TransferRequest(transfer.getId(), transfer.getRoundId(), transfer.getUserId(), transfer.getKind(),
            transfer.getAmount());
   }
}
