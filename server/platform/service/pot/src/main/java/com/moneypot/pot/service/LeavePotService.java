package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.LeavePotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.LeavePotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.RoundSupport;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;

/**
 * Leaves a pot and gets the contribution back (a refund through the wallet). It is only possible before bidding starts;
 * once it has, everyone stays until the pot is done. The owner cannot leave their own pot.
 */
@Service
@Validated
@RequiredArgsConstructor
public class LeavePotService implements LeavePotUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadMemberPort loadMemberPort;
   private final SaveMemberPort saveMemberPort;
   private final LoadRoundPort loadRoundPort;
   private final LoadTransferPort loadTransferPort;
   private final SaveTransferPort saveTransferPort;
   private final RoundSupport roundSupport;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(LeavePotCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (!pot.joiningIsOpen(Instant.now())) {
         throw BusinessRuleException.of("Bidding has started, so you cannot leave this pot any more");
      }
      if (pot.getOwnerId().equals(cmd.userId())) {
         throw BusinessRuleException.of("The owner cannot leave their own pot");
      }
      PotMember member = loadMemberPort.loadByPotId(pot.getId()).stream()
            .filter(m -> m.getUserId().equals(cmd.userId()) && !m.isLeft())
            .findFirst()
            .orElseThrow(() -> BusinessRuleException.of("You are not in this pot"));
      if (member.isPending()) {
         throw BusinessRuleException.of("Your payment is still being processed, try again in a moment");
      }

      Round joining = loadRoundPort.loadByPotIdAndNumber(pot.getId(), 1).orElseThrow();
      RoundTransfer paid = loadTransferPort.loadByRoundId(joining.getId()).stream()
            .filter(t -> t.getUserId().equals(cmd.userId()) && t.getKind() == TransferKind.CONTRIBUTION && t.isDone())
            .findFirst()
            .orElseThrow(() -> BusinessRuleException.of("There is no payment of yours to give back"));

      // the contribution is given back, so it no longer counts as paid
      paid.markVoid(PotAssembler.REFUNDED);
      saveTransferPort.save(paid);
      member.leave();
      saveMemberPort.save(member);
      PotChangeResult.TransferRequest refund =
            roundSupport.requestPayment(pot, joining, cmd.userId(), TransferKind.REFUND, paid.getAmount());
      notifyPotChangedPort.potChanged(pot.getId());
      return new PotChangeResult(potAssembler.assemble(pot, cmd.userId()), List.of(refund));
   }

   @Override
   public List<DomainEvent> buildEvents(LeavePotCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
