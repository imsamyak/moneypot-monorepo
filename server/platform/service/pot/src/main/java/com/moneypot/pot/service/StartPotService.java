package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.StartPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.StartPotCmd;
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
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs when the bidding start time of a pot has come (the scheduler calls it). Joining closes: the members who have paid
 * are the group and round 1 goes straight to bidding, because their contributions are already in the pot. Members whose
 * payment was still on its way are out (it is refunded if it does arrive). If fewer than two members paid, the pot is
 * cancelled and everyone gets their money back. Calling it again for a pot that has started changes nothing.
 */
@Service
@Validated
@RequiredArgsConstructor
public class StartPotService implements StartPotUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final SavePotPort savePotPort;
   private final LoadMemberPort loadMemberPort;
   private final SaveMemberPort saveMemberPort;
   private final LoadRoundPort loadRoundPort;
   private final SaveRoundPort saveRoundPort;
   private final LoadTransferPort loadTransferPort;
   private final SaveTransferPort saveTransferPort;
   private final RoundSupport roundSupport;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(StartPotCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (!pot.isOpen()) {
         return new PotChangeResult(potAssembler.assemble(pot, null), List.of());
      }
      Instant now = Instant.now();
      if (!pot.startTimeHasCome(now)) {
         throw BusinessRuleException.of("It is not time to start this pot yet");
      }

      List<PotMember> members = loadMemberPort.loadByPotId(pot.getId());
      for (PotMember member : members) {
         if (member.isPending()) {
            member.leave();
            saveMemberPort.save(member);
         }
      }
      List<PotMember> group = members.stream().filter(PotMember::isActive).toList();
      Round joining = loadRoundPort.loadByPotIdAndNumber(pot.getId(), 1).orElseThrow();

      List<PotChangeResult.TransferRequest> requests = new ArrayList<>();
      if (group.size() < Pot.MIN_MEMBERS_TO_START) {
         pot.cancel();
         for (PotMember member : group) {
            requests.add(refund(pot, joining, member));
         }
      } else {
         pot.start(group.size());
         joining.startBidding(now.plusSeconds(pot.getBidWindowSeconds()));
         saveRoundPort.save(joining);
      }
      Pot saved = savePotPort.save(pot);
      notifyPotChangedPort.potChanged(saved.getId());
      return new PotChangeResult(potAssembler.assemble(saved, null), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(StartPotCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }

   private PotChangeResult.TransferRequest refund(Pot pot, Round joining, PotMember member) {
      RoundTransfer paid = loadTransferPort.loadByRoundId(joining.getId()).stream()
            .filter(t -> t.getUserId().equals(member.getUserId()) && t.getKind() == TransferKind.CONTRIBUTION && t.isDone())
            .findFirst()
            .orElseThrow();
      paid.markVoid(PotAssembler.REFUNDED);
      saveTransferPort.save(paid);
      return roundSupport.requestPayment(pot, joining, member.getUserId(), TransferKind.REFUND, paid.getAmount());
   }
}
