package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Ends the bidding and decides the winner. The scheduler settles rounds whose bidding time is over ({@code early} =
 * false); the owner or an admin can settle sooner ({@code early} = true).
 */
public record SettleRoundCmd(
      @NotNull UUID potId,
      boolean early
) {
}
