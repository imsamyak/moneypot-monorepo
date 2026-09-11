package com.moneypot.wallet.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetAccountQuery(@NotNull UUID ownerId) {
}
