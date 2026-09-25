package com.moneypot.security;

import com.moneypot.shared.exception.security.ForbiddenException;
import com.moneypot.shared.exception.security.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardTest {

   private final UUID userId = UUID.randomUUID();

   @AfterEach
   void clear() {
      SecurityContext.clear();
   }

   private void callerIs(UserRole role, UUID user) {
      SecurityContext.setPrincipal(new UserPrincipal(user.toString(), "a@b.c", Set.of(role)));
   }

   @Test
   void anonymousCallerIsUnauthorized() {
      assertThatThrownBy(Caller::principal).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> OwnershipGuard.requireUser(userId)).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN)).isInstanceOf(UnauthorizedException.class);
   }

   @Test
   void userOwnershipAcceptsOnlyTheOwner() {
      callerIs(UserRole.ROLE_MEMBER, userId);
      assertThatCode(() -> OwnershipGuard.requireUser(userId)).doesNotThrowAnyException();
      assertThatThrownBy(() -> OwnershipGuard.requireUser(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void adminDoesNotPassUserOwnershipUnlessNamed() {
      callerIs(UserRole.ROLE_ADMIN, UUID.randomUUID());
      assertThatThrownBy(() -> OwnershipGuard.requireUser(userId)).isInstanceOf(ForbiddenException.class);
      assertThatCode(() -> OwnershipGuard.requireUserOr(userId, UserRole.ROLE_ADMIN)).doesNotThrowAnyException();
      assertThatThrownBy(() -> OwnershipGuard.requireUserOr(userId, UserRole.ROLE_MEMBER))
          .isInstanceOf(ForbiddenException.class);
   }

   @Test
   void roleGuardAcceptsOnlyListedRoles() {
      callerIs(UserRole.ROLE_MEMBER, userId);
      assertThatCode(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN, UserRole.ROLE_MEMBER)).doesNotThrowAnyException();
      assertThatThrownBy(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN)).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void callerReadsTheUserIdAndRole() {
      callerIs(UserRole.ROLE_MEMBER, userId);
      assertThat(Caller.userId()).isEqualTo(userId);
      assertThat(Caller.hasRole(UserRole.ROLE_MEMBER)).isTrue();
      assertThat(Caller.hasRole(UserRole.ROLE_ADMIN)).isFalse();
   }
}
