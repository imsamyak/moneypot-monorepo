package com.moneypot.pot.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Looks at a pot with its invite code, before joining it. */
public record GetPotByInviteQuery(
      @NotBlank String inviteCode,
      @NotNull UUID viewerId
) {
}
