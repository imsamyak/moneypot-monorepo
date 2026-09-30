package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.StartPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.StartPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.RoundSupport;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.exception.security.ForbiddenException;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Starts a full pot: round 1 opens and every member is asked for their contribution (events to the wallet). */
@Service
@Validated
@RequiredArgsConstructor
public class StartPotService implements StartPotUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadMemberPort loadMemberPort;
   private final SavePotPort savePotPort;
   private final RoundSupport roundSupport;
   private final PotAssembler potAssembler;
   private final NotifyPotChangedPort notifyPotChangedPort;

   @Override
   @Transactional
   public PotChangeResult execute(StartPotCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (!pot.getOwnerId().equals(cmd.userId())) {
         throw ForbiddenException.of("Only the owner can start the pot");
      }
      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      if (members.size() < pot.getMemberCount()) {
         throw BusinessRuleException.of("The pot needs " + pot.getMemberCount() + " members to start, it has " + members.size());
      }

      pot.start();
      Pot saved = savePotPort.save(pot);
      List<PotChangeResult.TransferRequest> requests = roundSupport.openRound(saved, members);
      notifyPotChangedPort.potChanged(saved.getId());
      return new PotChangeResult(potAssembler.assemble(saved, cmd.userId()), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(StartPotCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
