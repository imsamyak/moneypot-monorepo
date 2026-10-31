package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.port.in.usecase.CreatePotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.CreatePotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import com.moneypot.pot.port.out.persistence.SaveRoundPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.RoundSupport;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the pot, and its first round (where the members\' contributions are collected while joining is open). The owner
 * is the first member and pays at once like everyone else. The wallet opens the pot account when it hears Pot.Created.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CreatePotService implements CreatePotUseCase {

   private final SavePotPort savePotPort;
   private final SaveRoundPort saveRoundPort;
   private final SaveMemberPort saveMemberPort;
   private final RoundSupport roundSupport;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(CreatePotCmd cmd) {
      Pot pot = savePotPort.save(Pot.create(cmd.ownerId(), cmd.name(), cmd.contribution(), cmd.minBidPercent(),
            cmd.bidWindowSeconds(), cmd.startsAt(), Instant.now()));
      Round joining = saveRoundPort.save(Round.open(pot.getId(), 1));
      saveMemberPort.save(PotMember.join(pot.getId(), cmd.ownerId(), 1));
      PotChangeResult.TransferRequest payment =
            roundSupport.requestPayment(pot, joining, cmd.ownerId(), TransferKind.CONTRIBUTION, pot.getContribution());
      return new PotChangeResult(potAssembler.assemble(pot, cmd.ownerId()), List.of(payment));
   }

   @Override
   public List<DomainEvent> buildEvents(CreatePotCmd cmd, PotChangeResult result) {
      List<DomainEvent> events = new ArrayList<>();
      events.add(new com.moneypot.contract.pot.Pot.Created(result.pot().id().toString(), result.pot().name(), 0L));
      events.addAll(TransferEvents.of(result));
      return events;
   }
}
