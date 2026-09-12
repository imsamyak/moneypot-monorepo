package com.moneypot.wallet.service;

import com.moneypot.wallet.domain.exception.AccountNotFoundException;
import com.moneypot.wallet.port.in.usecase.GetAccountUseCase;
import com.moneypot.wallet.port.in.usecase.dto.query.GetAccountQuery;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;
import com.moneypot.wallet.port.out.persistence.LoadAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetAccountService implements GetAccountUseCase {

   private final LoadAccountPort loadAccountPort;

   @Override
   @Transactional(readOnly = true)
   public AccountResult execute(GetAccountQuery query) {
      return loadAccountPort.loadByOwnerId(query.ownerId())
            .map(AccountResult::from)
            .orElseThrow(() -> AccountNotFoundException.of(query.ownerId()));
   }
}
