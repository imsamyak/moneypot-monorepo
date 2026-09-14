package com.moneypot.wallet.adapter.in.web.controller.dto.request;

import com.moneypot.wallet.port.in.usecase.dto.command.DepositCmd;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** @param amount in paise */
public record DepositRequest(
      @Positive(message = "The amount must be greater than zero")
      @Max(value = DepositCmd.MAX_PAISE, message = "At most Rs 1,00,000 per deposit")
      long amount
) {

   public DepositCmd toCommand(UUID userId) {
      return new DepositCmd(userId, amount);
   }
}
