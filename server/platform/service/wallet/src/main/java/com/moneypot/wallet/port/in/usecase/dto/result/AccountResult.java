package com.moneypot.wallet.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;
import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.domain.entity.OwnerType;

import java.util.UUID;

public record AccountResult(UUID accountId, OwnerType ownerType, UUID ownerId, long balance)
      implements Result.One {

   public static AccountResult from(Account account) {
      return new AccountResult(account.getId(), account.getOwnerType(), account.getOwnerId(), account.getBalance());
   }
}
