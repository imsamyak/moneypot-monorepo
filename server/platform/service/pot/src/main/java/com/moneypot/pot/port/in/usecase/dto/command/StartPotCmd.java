package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Closes joining and starts round 1. Only the owner can do it, and only when the pot is full. */
public record StartPotCmd(
      @NotNull UUID potId,
      @NotNull UUID userId
) {
}
