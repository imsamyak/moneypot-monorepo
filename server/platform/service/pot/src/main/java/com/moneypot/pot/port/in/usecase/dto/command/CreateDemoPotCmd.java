package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Starts a practice pot for this user: four simulated members, play money, bidding open straight away. */
public record CreateDemoPotCmd(
      @NotNull UUID userId
) {
}
