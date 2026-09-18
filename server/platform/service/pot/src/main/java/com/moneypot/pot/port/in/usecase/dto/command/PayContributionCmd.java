package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Asks again for a contribution the wallet rejected, once the member has topped up. */
public record PayContributionCmd(
      @NotNull UUID potId,
      @NotNull UUID userId
) {
}
