package com.moneypot.wallet.port.in.usecase.dto.command;

import com.moneypot.wallet.domain.entity.OwnerType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Opens the account of a member or a pot; asking again for the same owner returns the account that exists. */
public record OpenAccountCmd(
      @NotNull OwnerType ownerType,
      @NotNull UUID ownerId
) {
}
