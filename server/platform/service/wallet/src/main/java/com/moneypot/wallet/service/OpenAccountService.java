package com.moneypot.wallet.service;

import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.port.in.usecase.OpenAccountUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.OpenAccountCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;
import com.moneypot.wallet.port.out.persistence.LoadAccountPort;
import com.moneypot.wallet.port.out.persistence.SaveAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Opens an account, or returns the one that exists. Two requests for the same owner can arrive together (a page that
 * loads twice); the unique owner id lets only one of them create the account, and the other one reads it back. That is
 * why this use case is not wrapped in one transaction: a failed insert would otherwise poison it.
 */
@Service
@Validated
@RequiredArgsConstructor
public class OpenAccountService implements OpenAccountUseCase {

   private final LoadAccountPort loadAccountPort;
   private final SaveAccountPort saveAccountPort;

   @Override
   public AccountResult execute(OpenAccountCmd cmd) {
      Account account = loadAccountPort.loadByOwnerId(cmd.ownerId()).orElse(null);
      if (account == null) {
         try {
            account = saveAccountPort.save(Account.open(cmd.ownerType(), cmd.ownerId()));
         } catch (DataIntegrityViolationException createdMeanwhile) {
            account = loadAccountPort.loadByOwnerId(cmd.ownerId()).orElseThrow(() -> createdMeanwhile);
         }
      }
      return AccountResult.from(account);
   }
}
