package com.moneypot.wallet.port.in.usecase.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** One page of an account history, newest first. */
public record GetLedgerQuery(
      @NotNull UUID ownerId,
      @Min(0) int page,
      @Min(1) @Max(100) int size
) {
}
