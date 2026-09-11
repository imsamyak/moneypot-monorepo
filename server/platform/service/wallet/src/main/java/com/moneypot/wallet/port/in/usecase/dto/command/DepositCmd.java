package com.moneypot.wallet.port.in.usecase.dto.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** Adds money to a member wallet. This is a development top-up: there is no bank behind it. */
public record DepositCmd(
      @NotNull UUID userId,
      @Positive @Max(DepositCmd.MAX_PAISE) long amount
) {

   /** Rs 1,00,000 per deposit. */
   public static final long MAX_PAISE = 10_000_000L;
}
