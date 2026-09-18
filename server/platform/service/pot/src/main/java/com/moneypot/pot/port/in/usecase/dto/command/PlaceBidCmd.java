package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** The amount, in paise, the member will accept for the pot this round. The lowest bid wins. */
public record PlaceBidCmd(
      @NotNull UUID potId,
      @NotNull UUID userId,
      @Positive long amount
) {
}
