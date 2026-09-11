package com.moneypot.wallet.port.in.usecase.dto.command;

import com.moneypot.contract.pot.Pot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Moves money between a member and a pot. The kind decides the direction: a contribution goes from the member to the
 * pot, a payout and a dividend go from the pot to the member. The reference makes the transfer run only once.
 */
public record TransferFundsCmd(
      @NotBlank String reference,
      @NotNull Pot.Kind kind,
      @NotNull UUID potId,
      @NotNull UUID roundId,
      @NotNull UUID userId,
      @Positive long amount
) {
}
