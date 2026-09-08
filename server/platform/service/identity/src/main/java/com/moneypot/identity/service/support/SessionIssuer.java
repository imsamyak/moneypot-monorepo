package com.moneypot.identity.service.support;

import com.moneypot.identity.config.IdentityProperties;
import com.moneypot.identity.domain.User;
import com.moneypot.identity.port.in.usecase.dto.result.SessionResult;
import com.moneypot.identity.service.mapper.UserResultMapper;
import com.moneypot.security.jwt.JwtSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Issues a session: one RS256 access token. Nothing is stored; when it expires the user signs in again. */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

   private final JwtSigner signer;
   private final IdentityProperties properties;

   public SessionResult start(User user) {
      long ttl = properties.jwt().accessTtlSeconds();
      String access = signer.sign(user.toPrincipal(), Duration.ofSeconds(ttl));
      return new SessionResult(access, ttl, UserResultMapper.toResult(user));
   }
}
