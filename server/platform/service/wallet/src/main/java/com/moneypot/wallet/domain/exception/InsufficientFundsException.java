package com.moneypot.wallet.domain.exception;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class InsufficientFundsException extends BusinessRuleException {

   private final UUID ownerId;
   private final long requested;
   private final long available;

   public InsufficientFundsException(UUID ownerId, long requested, long available) {
      super(
         String.format("Insufficient funds: requested %d paise, available %d paise", requested, available),
         Map.of("ownerId", String.valueOf(ownerId), "requested", requested, "available", available)
      );
      this.ownerId = ownerId;
      this.requested = requested;
      this.available = available;
   }

   public static InsufficientFundsException of(UUID ownerId, long requested, long available) {
      return new InsufficientFundsException(ownerId, requested, available);
   }
}
