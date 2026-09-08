package com.moneypot.identity.service;

import com.moneypot.identity.port.in.usecase.GetCurrentUserUseCase;
import com.moneypot.identity.port.in.usecase.dto.query.GetCurrentUserQuery;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;
import com.moneypot.identity.port.out.UserStore;
import com.moneypot.identity.service.mapper.UserResultMapper;
import com.moneypot.shared.exception.security.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class GetCurrentUserService implements GetCurrentUserUseCase {

   private final UserStore users;

   @Override
   public UserResult execute(GetCurrentUserQuery query) {
      try {
         return users.findById(UUID.fromString(query.userId()))
               .filter(user -> user.enabled())
               .map(UserResultMapper::toResult)
               .orElseThrow(() -> UnauthorizedException.of("The account no longer exists"));
      } catch (IllegalArgumentException invalidId) {
         throw UnauthorizedException.of("Invalid credentials");
      }
   }
}
