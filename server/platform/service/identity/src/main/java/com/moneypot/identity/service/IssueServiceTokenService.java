package com.moneypot.identity.service;

import com.moneypot.identity.config.IdentityProperties;
import com.moneypot.identity.port.in.usecase.IssueServiceTokenUseCase;
import com.moneypot.identity.port.in.usecase.dto.command.IssueServiceTokenCmd;
import com.moneypot.identity.port.in.usecase.dto.result.ServiceTokenResult;
import com.moneypot.shared.exception.security.UnauthorizedException;
import com.moneypot.security.jwt.JwtSigner;
import com.moneypot.security.UserPrincipal;
import com.moneypot.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@Validated
@RequiredArgsConstructor
public class IssueServiceTokenService implements IssueServiceTokenUseCase {

   private final IdentityProperties properties;
   private final JwtSigner signer;

   @Override
   public ServiceTokenResult execute(IssueServiceTokenCmd cmd) {
      String expected = cmd.clientId() == null ? null : properties.serviceClients().get(cmd.clientId());
      // Both secrets are hashed first so the comparison takes the same time whatever the input.
      boolean valid = expected != null && cmd.clientSecret() != null
            && MessageDigest.isEqual(sha256(expected), sha256(cmd.clientSecret()));
      if (!valid) {
         throw UnauthorizedException.of("Invalid client credentials");
      }
      long ttl = properties.jwt().serviceTtlSeconds();
      String token = signer.sign(UserPrincipal.of(cmd.clientId(), null, UserRole.ROLE_SYSTEM, null), Duration.ofSeconds(ttl));
      return new ServiceTokenResult(token, ttl);
   }

   private static byte[] sha256(String value) {
      try {
         return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException(e);
      }
   }
}
