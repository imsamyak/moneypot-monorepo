package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The bidding start time of a pot has come: close joining and start round 1 (or cancel it when too few joined). */
public record StartPotCmd(
      @NotNull UUID potId
) {
}
