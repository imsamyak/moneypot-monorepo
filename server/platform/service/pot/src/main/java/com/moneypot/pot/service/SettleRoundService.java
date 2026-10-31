package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.SettleRoundCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.service.support.DemoSupport;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.RoundSupport;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.security.OwnershipGuard;
import com.moneypot.security.UserRole;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;

/**
 * Ends the bidding: the lowest bid collects the pot, and the wallet is asked to pay the collector and to give the other
 * members their dividend. Normally the scheduler does this when the bidding time is over; the owner or an admin can do it
 * sooner. In a demo pot nothing goes to the wallet: the round is paid out at once and the next one opens.
 */
@Service
@Validated
@RequiredArgsConstructor
public class SettleRoundService implements SettleRoundUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadRoundPort loadRoundPort;
   private final LoadMemberPort loadMemberPort;
   private final RoundSupport roundSupport;
   private final DemoSupport demoSupport;
   private final NotifyPotChangedPort notifyPotChangedPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(SettleRoundCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (cmd.early()) {
         OwnershipGuard.requireUserOr(pot.getOwnerId(), UserRole.ROLE_ADMIN);
      }
      Instant now = Instant.now();
      Round round = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound())
            .filter(r -> pot.isActive() && r.getStatus() == RoundStatus.BIDDING)
            .orElseThrow(() -> BusinessRuleException.of("There is no bidding to settle"));
      if (!cmd.early() && !round.biddingHasEnded(now)) {
         throw BusinessRuleException.of("Bidding is still open");
      }

      List<PotMember> group = loadMemberPort.loadByPotId(pot.getId()).stream().filter(PotMember::isActive).toList();
      List<PotChangeResult.TransferRequest> requests = roundSupport.settle(pot, round, group);
      if (pot.isDemo()) {
         demoSupport.payOutAndAdvance(pot, round, group, now);
         requests = List.of();
      }
      notifyPotChangedPort.potChanged(pot.getId());
      return new PotChangeResult(potAssembler.assemble(pot, null), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(SettleRoundCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
