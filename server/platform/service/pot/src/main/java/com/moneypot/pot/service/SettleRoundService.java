package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.SettleRoundCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
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
 * Ends the bidding: the lowest bid wins, and the wallet is asked to pay the winner and to give the other members their
 * dividend. Normally the scheduler does this when the bidding time is over; the owner or an admin can do it sooner.
 */
@Service
@Validated
@RequiredArgsConstructor
public class SettleRoundService implements SettleRoundUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadRoundPort loadRoundPort;
   private final LoadMemberPort loadMemberPort;
   private final RoundSupport roundSupport;
   private final PotAssembler potAssembler;

   @Override
   @Transactional
   public PotChangeResult execute(SettleRoundCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      if (cmd.early()) {
         OwnershipGuard.requireUserOr(pot.getOwnerId(), UserRole.ROLE_ADMIN);
      }
      Round round = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound())
            .filter(r -> pot.isActive() && r.getStatus() == RoundStatus.BIDDING)
            .orElseThrow(() -> BusinessRuleException.of("There is no bidding to settle"));
      if (!cmd.early() && !round.biddingHasEnded(Instant.now())) {
         throw BusinessRuleException.of("Bidding is still open");
      }

      List<PotChangeResult.TransferRequest> requests =
            roundSupport.settle(pot, round, loadMemberPort.loadByPotId(pot.getId()));
      return new PotChangeResult(potAssembler.assemble(pot, null), requests);
   }

   @Override
   public List<DomainEvent> buildEvents(SettleRoundCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
