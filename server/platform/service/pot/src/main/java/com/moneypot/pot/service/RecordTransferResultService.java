package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.RoundTransfer;
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
 * Moves the round forward when the wallet answers a transfer. This is where the rounds advance:
 * <ul>
 *   <li>every contribution paid: bidding opens (or, when only one member is left, that member wins at once);</li>
 *   <li>the payout and every dividend paid: the round is settled, the winner is marked, and the next round opens
 *       (or the pot is completed when every member has won).</li>
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
   private final PotAssembler potAssembler;
   private final NotifyPotChangedPort notifyPotChangedPort;

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

      if (cmd.completed()) {
         transfer.markDone();
      } else {
         transfer.markRejected(cmd.reason());
         log.info("Transfer {} of pot {} was rejected: {}", transfer.getId(), pot.getId(), cmd.reason());
      }
      saveTransferPort.save(transfer);
      notifyPotChangedPort.potChanged(pot.getId());

      List<PotChangeResult.TransferRequest> requests = new ArrayList<>();
      if (cmd.completed()) {
         Round round = loadRoundPort.loadById(transfer.getRoundId()).orElseThrow();
         boolean everythingPaid = loadTransferPort.loadByRoundId(round.getId()).stream().allMatch(RoundTransfer::isDone);
         if (everythingPaid && round.getStatus() == RoundStatus.COLLECTING) {
            requests.addAll(contributionsAreIn(pot, round));
         } else if (everythingPaid && round.getStatus() == RoundStatus.SETTLING) {
            requests.addAll(roundIsPaid(pot, round));
         }
      }
      return new PotChangeResult(potAssembler.assemble(pot, null), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(RecordTransferResultCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }

   /** The pot is full: open the bidding, unless only one member is left to win, who is paid straight away. */
   private List<PotChangeResult.TransferRequest> contributionsAreIn(Pot pot, Round round) {
      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      if (members.stream().filter(member -> !member.isWon()).count() == 1) {
         return roundSupport.settle(pot, round, members);
      }
      round.startBidding(Instant.now().plusSeconds(pot.getBidWindowSeconds()));
      saveRoundPort.save(round);
      return List.of();
   }

   /** The winner and the other members are paid: close the round and start the next one, or finish the pot. */
   private List<PotChangeResult.TransferRequest> roundIsPaid(Pot pot, Round round) {
      round.settled();
      saveRoundPort.save(round);

      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      members.stream()
            .filter(member -> member.getUserId().equals(round.getWinnerId()))
            .findFirst()
            .ifPresent(winner -> {
               winner.markWon(round.getNumber());
               saveMemberPort.save(winner);
            });

      if (members.stream().allMatch(PotMember::isWon)) {
         pot.complete();
         savePotPort.save(pot);
         return List.of();
      }
      pot.nextRound();
      Pot saved = savePotPort.save(pot);
      return roundSupport.openRound(saved, members);
   }
}
