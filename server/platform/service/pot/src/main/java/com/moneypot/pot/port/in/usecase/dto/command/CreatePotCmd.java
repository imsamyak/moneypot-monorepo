package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Creates a pot; the owner becomes its first member. Money is in paise. The domain checks the limits. */
public record CreatePotCmd(
      @NotNull UUID ownerId,
      @NotBlank String name,
      int memberCount,
      long contribution,
      int minBidPercent,
      int bidWindowSeconds
) {
}
