package com.moneypot.identity.adapter.in.web.controller.dto.response;

import com.moneypot.identity.port.in.usecase.dto.result.UserResult;

public record UserResponse(String id, String email, String role) {
   public static UserResponse from(UserResult result) {
      return new UserResponse(result.id(), result.email(), result.role());
   }
}
