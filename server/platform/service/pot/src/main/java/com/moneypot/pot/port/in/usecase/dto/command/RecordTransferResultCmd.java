package com.moneypot.pot.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** The wallet answered a transfer the round asked for. */
public record RecordTransferResultCmd(
      @NotNull UUID transferId,
      boolean completed,
      String reason
) {
}
