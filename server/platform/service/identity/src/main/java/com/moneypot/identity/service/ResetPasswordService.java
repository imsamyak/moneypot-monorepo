package com.moneypot.identity.service;

import com.moneypot.identity.domain.PasswordResetToken;
import com.moneypot.identity.domain.User;
import com.moneypot.identity.port.in.usecase.ResetPasswordUseCase;
import com.moneypot.identity.port.in.usecase.dto.command.ResetPasswordCmd;
import com.moneypot.identity.port.in.usecase.dto.result.Done;
import com.moneypot.identity.port.out.PasswordHasher;
import com.moneypot.identity.port.out.PasswordResetStore;
import com.moneypot.identity.port.out.UserStore;
import com.moneypot.identity.service.support.CredentialRules;
import com.moneypot.identity.service.support.ResetTokens;
import com.moneypot.shared.exception.validation.InvalidCommandException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;

/**
 * Sets a new password for the holder of a valid reset link. An unknown, expired or already used link all fail with the
 * same message. Using a link removes every pending link of that user and lifts any lockout.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ResetPasswordService implements ResetPasswordUseCase {

   private static final String INVALID_LINK = "The reset link is invalid or has expired";

   private final UserStore users;
   private final PasswordResetStore resets;
   private final PasswordHasher hasher;
   private final Clock clock;

   @Override
   @Transactional
   public Done execute(ResetPasswordCmd cmd) {
      CredentialRules.validatePassword(cmd.newPassword());
      Instant now = clock.instant();

      PasswordResetToken token = resets.findByTokenHash(ResetTokens.hash(cmd.token()))
         .filter(found -> found.isUsableAt(now))
         .orElseThrow(() -> InvalidCommandException.of(INVALID_LINK));
      User user = users.findById(token.userId())
         .filter(User::enabled)
         .orElseThrow(() -> InvalidCommandException.of(INVALID_LINK));

      user.changePassword(hasher.hash(cmd.newPassword()));
      users.save(user);
      resets.deleteByUserId(user.id());
      return Done.INSTANCE;
   }
}
