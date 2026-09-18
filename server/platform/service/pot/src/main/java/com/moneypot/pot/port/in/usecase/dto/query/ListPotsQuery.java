package com.moneypot.pot.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** @param mineOnly true for the pots the user belongs to; false for the pots still open to join */
public record ListPotsQuery(
      @NotNull UUID userId,
      boolean mineOnly
) {
}
