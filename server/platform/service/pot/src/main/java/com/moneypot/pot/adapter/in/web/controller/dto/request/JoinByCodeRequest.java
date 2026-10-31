package com.moneypot.pot.adapter.in.web.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The invite code someone was given. It alone finds the pot. */
public record JoinByCodeRequest(
      @NotBlank(message = "Enter the invite code")
      @Size(max = 64, message = "That is not an invite code")
      String inviteCode
) {
}
