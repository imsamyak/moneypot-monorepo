package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Leaves a pot before bidding has started and gets the contribution back. */
public record LeavePotCmd(
      @NotNull UUID potId,
      @NotNull UUID userId
) {
}
