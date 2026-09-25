package com.moneypot.identity.service.mapper;

import com.moneypot.identity.domain.User;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;

public final class UserResultMapper {

   private UserResultMapper() {
   }

   public static UserResult toResult(User user) {
      return new UserResult(user.id().toString(), user.email(), user.role().name());
   }
}
