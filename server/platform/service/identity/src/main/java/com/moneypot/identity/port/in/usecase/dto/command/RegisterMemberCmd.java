package com.moneypot.identity.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;

public record RegisterMemberCmd(
      @NotBlank String email,
      @NotBlank String password
) {
}
