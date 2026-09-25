package com.moneypot.security;

import com.moneypot.shared.exception.security.ForbiddenException;
import com.moneypot.shared.exception.security.UnauthorizedException;

import java.util.UUID;

/**
 * Reads the caller of the current request from {@link SecurityContext}, so call it from request threads only.
 * It only reads; it never decides who may do what (see {@link RoleGuard} and {@link OwnershipGuard}).
 */
public final class Caller {

   private Caller() {
   }

   /**
    * @throws UnauthorizedException if the request is anonymous
    */
   public static UserPrincipal principal() {
      UserPrincipal principal = SecurityContext.getPrincipal();
      if (principal == null) {
         throw UnauthorizedException.of("Authentication is required");
      }
      return principal;
   }

   public static boolean hasRole(UserRole role) {
      UserPrincipal principal = SecurityContext.getPrincipal();
      return principal != null && principal.hasRole(role);
   }

   /**
    * @throws UnauthorizedException if the request is anonymous or the user id is not a valid UUID
    */
   public static UUID userId() {
      return parse(principal().userId(), "userId");
   }

   private static UUID parse(String value, String what) {
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException | NullPointerException ex) {
         throw UnauthorizedException.of("Invalid " + what + " in credentials");
      }
   }
}
