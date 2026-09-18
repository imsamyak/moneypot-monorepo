package com.moneypot.pot.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** @param viewerId who is looking: only the members of a pot see its bids */
public record GetPotQuery(
      @NotNull UUID potId,
      @NotNull UUID viewerId
) {
}
