package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.JoinPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Adds a member to a pot that has not started. The pot row is locked, so two people cannot take the last seat. */
@Service
@Validated
@RequiredArgsConstructor
public class JoinPotService implements JoinPotUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadMemberPort loadMemberPort;
   private final SaveMemberPort saveMemberPort;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotResult execute(JoinPotCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (!pot.isOpen()) {
         throw BusinessRuleException.of("The pot has already started");
      }
      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      if (members.stream().anyMatch(member -> member.getUserId().equals(cmd.userId()))) {
         throw BusinessRuleException.of("You are already in this pot");
      }
      if (members.size() >= pot.getMemberCount()) {
         throw BusinessRuleException.of("The pot is full");
      }
      saveMemberPort.save(PotMember.join(pot.getId(), cmd.userId(), members.size() + 1));
      notifyPotChangedPort.potChanged(pot.getId());
      return potAssembler.assemble(pot, cmd.userId());
   }
}
