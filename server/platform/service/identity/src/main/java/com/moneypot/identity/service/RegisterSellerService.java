package com.moneypot.identity.service;

import com.moneypot.identity.domain.User;
import com.moneypot.identity.port.in.usecase.CreateUserUseCase;
import com.moneypot.identity.port.in.usecase.RegisterSellerUseCase;
import com.moneypot.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.moneypot.identity.port.in.usecase.dto.command.RegisterSellerCmd;
import com.moneypot.identity.port.in.usecase.dto.result.SessionResult;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;
import com.moneypot.identity.port.out.UserStore;
import com.moneypot.identity.service.support.SessionIssuer;
import com.moneypot.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class RegisterSellerService implements RegisterSellerUseCase {

   private final CreateUserUseCase createUser;
   private final UserStore users;
   private final SessionIssuer sessions;

   @Override
   public SessionResult execute(RegisterSellerCmd cmd) {
      UserResult created = createUser.execute(new CreateUserCmd(cmd.email(), cmd.password(), UserRole.ROLE_SELLER));
      User user = users.findById(UUID.fromString(created.id())).orElseThrow();
      return sessions.start(user);
   }
}
