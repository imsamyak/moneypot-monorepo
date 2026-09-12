package com.moneypot.wallet.service;

import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.domain.entity.EntryKind;
import com.moneypot.wallet.domain.entity.EntryType;
import com.moneypot.wallet.domain.entity.LedgerEntry;
import com.moneypot.wallet.domain.entity.OwnerType;
import com.moneypot.wallet.port.in.usecase.DepositUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.DepositCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;
import com.moneypot.wallet.port.out.persistence.LoadAccountForUpdatePort;
import com.moneypot.wallet.port.out.persistence.SaveAccountPort;
import com.moneypot.wallet.port.out.persistence.SaveLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class DepositService implements DepositUseCase {

   private final LoadAccountForUpdatePort loadAccountForUpdatePort;
   private final SaveAccountPort saveAccountPort;
   private final SaveLedgerPort saveLedgerPort;

   @Override
   @Transactional
   public AccountResult execute(DepositCmd cmd) {
      Account account = loadAccountForUpdatePort.loadByOwnerIdsForUpdate(List.of(cmd.userId())).stream()
            .findFirst()
            .orElseGet(() -> saveAccountPort.save(Account.open(OwnerType.USER, cmd.userId())));

      account.credit(cmd.amount());
      Account saved = saveAccountPort.save(account);
      saveLedgerPort.saveAll(List.of(LedgerEntry.record(
            saved, EntryType.CREDIT, EntryKind.DEPOSIT, cmd.amount(), "deposit-" + UUID.randomUUID(), null, Instant.now())));
      return AccountResult.from(saved);
   }
}
