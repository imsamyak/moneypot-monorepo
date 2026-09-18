package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Adds a member to a pot that has not started. */
public record JoinPotCmd(
      @NotNull UUID potId,
      @NotNull UUID userId
) {
}
