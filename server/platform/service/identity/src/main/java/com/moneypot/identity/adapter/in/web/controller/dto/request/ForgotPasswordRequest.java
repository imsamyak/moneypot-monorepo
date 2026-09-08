package com.moneypot.identity.adapter.in.web.controller.dto.request;

import com.moneypot.identity.port.in.usecase.dto.command.ForgotPasswordCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Size(max = 254) String email) {

   public ForgotPasswordCmd toCommand() {
      return new ForgotPasswordCmd(email);
   }
}
