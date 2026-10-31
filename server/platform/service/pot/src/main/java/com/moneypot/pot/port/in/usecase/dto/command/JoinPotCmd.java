package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Joins a pot that has not started; the contribution is taken from the wallet at once. Everyone but the owner needs the
 * invite code, and the code alone is enough to find the pot: {@code potId} may be left out.
 */
public record JoinPotCmd(
      UUID potId,
      @NotNull UUID userId,
      String inviteCode
) {
}
