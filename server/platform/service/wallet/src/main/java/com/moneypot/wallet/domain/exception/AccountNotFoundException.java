package com.moneypot.wallet.domain.exception;

import com.moneypot.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AccountNotFoundException extends EntityNotFoundException {

   private final UUID ownerId;

   public AccountNotFoundException(UUID ownerId) {
      super("Account", ownerId);
      this.ownerId = ownerId;
   }

   public static AccountNotFoundException of(UUID ownerId) {
      return new AccountNotFoundException(ownerId);
   }
}
