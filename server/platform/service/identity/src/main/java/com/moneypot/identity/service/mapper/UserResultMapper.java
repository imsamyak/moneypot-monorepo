package com.moneypot.identity.service.mapper;

import com.moneypot.identity.domain.User;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;
import com.moneypot.security.UserRole;

public final class UserResultMapper {

   private UserResultMapper() {
   }

   public static UserResult toResult(User user) {
      String sellerId = user.role() == UserRole.ROLE_SELLER ? user.id().toString() : null;
      return new UserResult(user.id().toString(), user.email(), user.role().name(), sellerId);
   }
}
