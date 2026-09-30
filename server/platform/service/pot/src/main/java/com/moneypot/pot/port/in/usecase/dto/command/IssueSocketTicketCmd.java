package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** A member asks to watch their pot live. */
public record IssueSocketTicketCmd(
      @NotNull UUID potId,
      @NotNull UUID userId
) {
}
