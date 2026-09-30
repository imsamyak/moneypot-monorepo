package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.Round;
import com.moneypot.pot.domain.entity.RoundStatus;
import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.PayContributionUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.PayContributionCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import com.moneypot.pot.service.support.PotAssembler;
import com.moneypot.pot.service.support.TransferEvents;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Asks the wallet again for a contribution it rejected (usually because the balance was too low). The transfer keeps
 * its reference, so a contribution that did go through can never be taken twice.
 */
@Service
@Validated
@RequiredArgsConstructor
public class PayContributionService implements PayContributionUseCase {

   private final LoadPotForUpdatePort loadPotForUpdatePort;
   private final LoadRoundPort loadRoundPort;
   private final LoadTransferPort loadTransferPort;
   private final SaveTransferPort saveTransferPort;
   private final PotAssembler potAssembler;
   private final NotifyPotChangedPort notifyPotChangedPort;

   @Override
   @Transactional
   public PotChangeResult execute(PayContributionCmd cmd) {
      Pot pot = loadPotForUpdatePort.loadByIdForUpdate(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      Round round = loadRoundPort.loadByPotIdAndNumber(pot.getId(), pot.getCurrentRound())
            .filter(r -> pot.isActive() && r.getStatus() == RoundStatus.COLLECTING)
            .orElseThrow(() -> BusinessRuleException.of("No contribution is being collected right now"));

      RoundTransfer transfer = loadTransferPort.loadByRoundId(round.getId()).stream()
            .filter(t -> t.getKind() == TransferKind.CONTRIBUTION && t.getUserId().equals(cmd.userId()))
            .findFirst()
            .orElseThrow(() -> BusinessRuleException.of("You have no contribution in this round"));

      transfer.retry();
      saveTransferPort.save(transfer);
      notifyPotChangedPort.potChanged(pot.getId());
      return new PotChangeResult(potAssembler.assemble(pot, cmd.userId()),
            List.of(new PotChangeResult.TransferRequest(transfer.getId(), round.getId(), transfer.getUserId(),
                  transfer.getKind(), transfer.getAmount())));
   }

   @Override
   public List<DomainEvent> buildEvents(PayContributionCmd cmd, PotChangeResult result) {
      return TransferEvents.of(result);
   }
}
