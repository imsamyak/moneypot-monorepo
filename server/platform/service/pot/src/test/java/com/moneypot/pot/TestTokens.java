package com.moneypot.pot;

import com.moneypot.security.UserPrincipal;
import com.moneypot.security.UserRole;
import com.moneypot.security.jwt.JwtSigner;
import com.moneypot.security.jwt.RsaKeys;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.security.KeyPair;
import java.time.Duration;
import java.util.UUID;

/**
 * Access tokens for the tests, signed with a key generated for the test run. The service verifies them like the ones
 * identity issues: only the public key is configured.
 */
public final class TestTokens {

   private static final KeyPair KEYS = RsaKeys.generate();
   private static final JwtSigner SIGNER = JwtSigner.forKey(KEYS.getPrivate(), "moneypot-identity");

   private TestTokens() {
   }

   public static void register(DynamicPropertyRegistry registry) {
      registry.add("moneypot.security.jwt.public-key", () -> RsaKeys.toPem(KEYS.getPublic()));
   }

   public static String member(UUID userId) {
      return token(UserRole.ROLE_MEMBER, userId);
   }

   public static String admin() {
      return token(UserRole.ROLE_ADMIN, UUID.randomUUID());
   }

   private static String token(UserRole role, UUID userId) {
      return SIGNER.sign(UserPrincipal.of(userId.toString(), "test@example.com", role, null), Duration.ofMinutes(5));
   }
}
