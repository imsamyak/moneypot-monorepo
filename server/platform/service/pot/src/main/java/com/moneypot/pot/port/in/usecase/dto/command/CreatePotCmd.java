package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates a pot; the owner is the first member and pays their contribution at once. Money is in paise. The domain
 * checks the limits.
 *
 * @param startsAt when joining closes and bidding begins
 */
public record CreatePotCmd(
      @NotNull UUID ownerId,
      @NotBlank String name,
      long contribution,
      int minBidPercent,
      int bidWindowSeconds,
      @NotNull Instant startsAt
) {
}
