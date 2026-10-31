package com.moneypot.pot.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The pots a user belongs to. Pots are by invitation only, so there is no list of other pots. */
public record ListPotsQuery(
      @NotNull UUID userId
) {
}
