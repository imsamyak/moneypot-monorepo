package com.moneypot.wallet.service;

import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.domain.exception.AccountNotFoundException;
import com.moneypot.wallet.port.in.usecase.GetLedgerUseCase;
import com.moneypot.wallet.port.in.usecase.dto.query.GetLedgerQuery;
import com.moneypot.wallet.port.in.usecase.dto.result.LedgerEntryResult;
import com.moneypot.wallet.port.in.usecase.dto.result.LedgerPageResult;
import com.moneypot.wallet.port.out.persistence.LedgerPage;
import com.moneypot.wallet.port.out.persistence.LoadAccountPort;
import com.moneypot.wallet.port.out.persistence.LoadLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetLedgerService implements GetLedgerUseCase {

   private final LoadAccountPort loadAccountPort;
   private final LoadLedgerPort loadLedgerPort;

   @Override
   @Transactional(readOnly = true)
   public LedgerPageResult execute(GetLedgerQuery query) {
      Account account = loadAccountPort.loadByOwnerId(query.ownerId())
            .orElseThrow(() -> AccountNotFoundException.of(query.ownerId()));
      LedgerPage page = loadLedgerPort.loadPage(account.getId(), query.page(), query.size());
      return new LedgerPageResult(
            page.entries().stream().map(LedgerEntryResult::from).toList(),
            page.hasMore() ? String.valueOf(query.page() + 1) : null);
   }
}
