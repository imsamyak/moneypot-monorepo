package com.moneypot.identity.adapter.in.web.controller.dto.request;

import com.moneypot.identity.port.in.usecase.dto.command.RegisterMemberCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
      @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(max = 200) String password
) {
   public RegisterMemberCmd toCommand() {
      return new RegisterMemberCmd(email, password);
   }
}
