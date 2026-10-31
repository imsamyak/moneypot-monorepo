package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.RecordTransferResultUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.RecordTransferResultCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.port.out.persistence.SaveRoundPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.RoundSupport;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the wallet\'s answer to a transfer the pot asked for.
 * <ul>
 *   <li><b>A member\'s join payment</b> (round 1, before bidding): paid, the member is in; refused, the member is out. A
 *       payment that arrives after bidding started (or after the member left) is given straight back.</li>
 *   <li><b>Later rounds</b>: when every contribution is paid, bidding opens (or, when only one member is left, that member
 *       wins at once); when the payout and every dividend are paid, the round is settled, the winner is marked, and the
 *       next round opens (or the pot is completed when every member has won).</li>
 * </ul>
 * An answer that was already applied changes nothing, so a replayed event is harmless.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RecordTransferResultService implements RecordTransferResultUseCase {

   private final LoadTransferPort loadTransferPort;
   private final SaveTransferPort saveTransferPort;
   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final SavePotPort savePotPort;
   private final LoadRoundPort loadRoundPort;
   private final SaveRoundPort saveRoundPort;
   private final LoadMemberPort loadMemberPort;
   private final SaveMemberPort saveMemberPort;
   private final RoundSupport roundSupport;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(RecordTransferResultCmd cmd) {
      RoundTransfer transfer = loadTransferPort.loadById(cmd.transferId()).orElse(null);
      if (transfer == null) {
         log.warn("The wallet answered an unknown transfer {}", cmd.transferId());
         return new PotChangeResult(null, List.of());
      }
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(transfer.getPotId())
            .orElseThrow(() -> PotNotFoundException.of(transfer.getPotId()));
      if (!transfer.isRequested()) {
         return new PotChangeResult(null, List.of());
      }

      Round round = loadRoundPort.loadById(transfer.getRoundId()).orElseThrow();
      PotMember member = loadMemberPort.loadByPotId(pot.getId()).stream()
            .filter(m -> m.getUserId().equals(transfer.getUserId()))
            .findFirst()
            .orElse(null);
      boolean joinPayment = transfer.getKind() == TransferKind.CONTRIBUTION && round.getNumber() == 1;
      List<PotChangeResult.TransferRequest> requests = new ArrayList<>();

      if (cmd.completed()) {
         transfer.markDone();
         saveTransferPort.save(transfer);
         if (joinPayment) {
            requests.addAll(joinPaymentArrived(pot, round, transfer, member));
         }
      } else {
         log.info("Transfer {} of pot {} was rejected: {}", transfer.getId(), pot.getId(), cmd.reason());
         if (joinPayment && member != null && member.isPending()) {
            // the payment was refused, so the member is not in
            member.leave();
            saveMemberPort.save(member);
            transfer.markVoid(cmd.reason());
         } else {
            transfer.markRejected(cmd.reason());
         }
         saveTransferPort.save(transfer);
      }

      if (cmd.completed() && pot.isActive()) {
         boolean everythingPaid = loadTransferPort.loadByRoundId(round.getId()).stream()
               .filter(t -> !t.isVoid())
               .allMatch(RoundTransfer::isDone);
         List<PotMember> group = loadMemberPort.loadByPotId(pot.getId()).stream().filter(PotMember::isActive).toList();
         if (everythingPaid && round.getStatus() == RoundStatus.COLLECTING) {
            requests.addAll(contributionsAreIn(pot, round, group));
         } else if (everythingPaid && round.getStatus() == RoundStatus.SETTLING) {
            requests.addAll(roundIsPaid(pot, round, group));
         }
      }
      notifyPotChangedPort.potChanged(pot.getId());
      return new PotChangeResult(potAssembler.assemble(pot, null), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(RecordTransferResultCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }

   /** A member\'s contribution reached the pot: they are in, unless it came too late, when it is given back. */
   private List<PotChangeResult.TransferRequest> joinPaymentArrived(Pot pot, Round round, RoundTransfer transfer, PotMember member) {
      if (member != null && member.isPending() && pot.isOpen()) {
         member.activate();
         saveMemberPort.save(member);
         return List.of();
      }
      if (member != null && member.isActive()) {
         return List.of();
      }
      // bidding already started, or the member left: the money goes back
      transfer.markVoid(PotAssembler.REFUNDED);
      saveTransferPort.save(transfer);
      return List.of(roundSupport.requestPayment(pot, round, transfer.getUserId(), TransferKind.REFUND, transfer.getAmount()));
   }

   /** Every contribution of the round is in: open the bidding, unless only one member is left to win, who is paid at once. */
   private List<PotChangeResult.TransferRequest> contributionsAreIn(Pot pot, Round round, List<PotMember> group) {
      if (group.stream().filter(member -> !member.isWon()).count() == 1) {
         return roundSupport.settle(pot, round, group);
      }
      round.startBidding(Instant.now().plusSeconds(pot.getBidWindowSeconds()));
      saveRoundPort.save(round);
      return List.of();
   }

   /** The winner and the other members are paid: close the round and start the next one, or finish the pot. */
   private List<PotChangeResult.TransferRequest> roundIsPaid(Pot pot, Round round, List<PotMember> group) {
      round.settled();
      saveRoundPort.save(round);

      group.stream()
            .filter(member -> member.getUserId().equals(round.getWinnerId()))
            .findFirst()
            .ifPresent(winner -> {
               winner.markWon(round.getNumber());
               saveMemberPort.save(winner);
            });

      if (group.stream().allMatch(PotMember::isWon)) {
         pot.complete();
         savePotPort.save(pot);
         return List.of();
      }
      pot.nextRound();
      Pot saved = savePotPort.save(pot);
      return roundSupport.openRound(saved, group);
   }
}
