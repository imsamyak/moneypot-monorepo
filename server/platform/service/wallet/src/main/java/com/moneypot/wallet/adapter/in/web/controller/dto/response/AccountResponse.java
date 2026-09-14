package com.moneypot.wallet.adapter.in.web.controller.dto.response;

import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;

import java.util.UUID;

/** @param balance in paise */
public record AccountResponse(UUID accountId, long balance) {

   public static AccountResponse from(AccountResult result) {
      return new AccountResponse(result.accountId(), result.balance());
   }
}
