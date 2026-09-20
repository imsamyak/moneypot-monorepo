package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.port.in.usecase.CreatePotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.CreatePotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Creates the pot and makes its owner the first member. The wallet opens the pot account when it hears Pot.Created. */
@Service
@Validated
@RequiredArgsConstructor
public class CreatePotService implements CreatePotUseCase {

   private final SavePotPort savePotPort;
   private final SaveMemberPort saveMemberPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(CreatePotCmd cmd) {
      Pot pot = savePotPort.save(Pot.create(cmd.ownerId(), cmd.name(), cmd.memberCount(), cmd.contribution(),
            cmd.minBidPercent(), cmd.bidWindowSeconds()));
      saveMemberPort.save(PotMember.join(pot.getId(), cmd.ownerId(), 1));
      return new PotChangeResult(potAssembler.assemble(pot, cmd.ownerId()), List.of());
   }

   @Override
   public List<DomainEvent> buildEvents(CreatePotCmd cmd, PotChangeResult result) {
      return List.of(new com.moneypot.contract.pot.Pot.Created(result.pot().id().toString(), result.pot().name(), 0L));
   }
}
