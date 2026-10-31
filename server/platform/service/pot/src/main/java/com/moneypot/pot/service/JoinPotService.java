package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.exception.InviteNotFoundException;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.JoinPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Joins a pot by invitation, before bidding starts. The member is pending until the wallet confirms their contribution:
 * a member whose payment is refused is not in. The pot row is locked, so the member limit cannot be passed by a race.
 */
@Service
@Validated
@RequiredArgsConstructor
public class JoinPotService implements JoinPotUseCase {

   private final LoadPotPort loadPotPort;
   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadMemberPort loadMemberPort;
   private final SaveMemberPort saveMemberPort;
   private final LoadRoundPort loadRoundPort;
   private final RoundSupport roundSupport;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(JoinPotCmd cmd) {
      UUID potId = cmd.potId() != null ? cmd.potId() : loadPotPort.loadByInviteCode(cmd.inviteCode() == null ? "" : cmd.inviteCode())
            .orElseThrow(InviteNotFoundException::of).getId();
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(potId).orElseThrow(() -> PotNotFoundException.of(potId));
      if (!pot.joiningIsOpen(Instant.now())) {
         throw BusinessRuleException.of("Joining is closed: bidding has started");
      }
      boolean owner = pot.getOwnerId().equals(cmd.userId());
      if (!owner && !pot.getInviteCode().equals(cmd.inviteCode())) {
         throw ForbiddenException.of("You need an invitation to join this pot");
      }

      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      PotMember existing = members.stream().filter(m -> m.getUserId().equals(cmd.userId())).findFirst().orElse(null);
      if (existing != null && !existing.isLeft()) {
         throw BusinessRuleException.of("You are already in this pot");
      }
      if (members.stream().filter(m -> !m.isLeft()).count() >= Pot.MAX_MEMBERS) {
         throw BusinessRuleException.of("The pot is full");
      }

      if (existing == null) {
         saveMemberPort.save(PotMember.join(pot.getId(), cmd.userId(), members.size() + 1));
      } else {
         existing.rejoin();
         saveMemberPort.save(existing);
      }
      Round joining = loadRoundPort.loadByPotIdAndNumber(pot.getId(), 1).orElseThrow();
      PotChangeResult.TransferRequest payment =
            roundSupport.requestPayment(pot, joining, cmd.userId(), TransferKind.CONTRIBUTION, pot.getContribution());
      notifyPotChangedPort.potChanged(pot.getId());
      return new PotChangeResult(potAssembler.assemble(pot, cmd.userId()), List.of(payment));
   }

   @Override
   public List<DomainEvent> buildEvents(JoinPotCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
