package com.moneypot.identity.adapter.in.web.controller;

import com.moneypot.identity.adapter.in.web.controller.dto.request.ForgotPasswordRequest;
import com.moneypot.identity.adapter.in.web.controller.dto.request.LoginRequest;
import com.moneypot.identity.adapter.in.web.controller.dto.request.RegisterRequest;
import com.moneypot.identity.adapter.in.web.controller.dto.request.ResetPasswordRequest;
import com.moneypot.identity.adapter.in.web.controller.dto.response.SessionResponse;
import com.moneypot.identity.adapter.in.web.controller.dto.response.UserResponse;
import com.moneypot.identity.port.in.usecase.ForgotPasswordUseCase;
import com.moneypot.identity.port.in.usecase.GetCurrentUserUseCase;
import com.moneypot.identity.port.in.usecase.LoginUseCase;
import com.moneypot.identity.port.in.usecase.RegisterMemberUseCase;
import com.moneypot.identity.port.in.usecase.ResetPasswordUseCase;
import com.moneypot.identity.port.in.usecase.dto.query.GetCurrentUserQuery;
import com.moneypot.security.Caller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nothing here is meant to be called from a browser directly (the storefront server calls it), so CORS is not enabled.
 */
@RestController
@RequiredArgsConstructor
public class IdentityController {

   private final RegisterMemberUseCase registerMemberUseCase;
   private final LoginUseCase loginUseCase;
   private final GetCurrentUserUseCase getCurrentUserUseCase;
   private final ForgotPasswordUseCase forgotPasswordUseCase;
   private final ResetPasswordUseCase resetPasswordUseCase;

   @PostMapping("/auth/register")
   @ResponseStatus(HttpStatus.CREATED)
   public SessionResponse register(@Valid @RequestBody RegisterRequest request) {
      return SessionResponse.from(registerMemberUseCase.execute(request.toCommand()));
   }

   @PostMapping("/auth/login")
   public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
      return noStore(SessionResponse.from(loginUseCase.execute(request.toCommand())));
   }

   /**
    * Access tokens are stateless, so signing out means the caller discards its token (the storefront server clears
    * its cookie); there is nothing to revoke on the server until refresh tokens exist. It needs no token itself, so a
    * caller whose token has already expired can still sign out.
    */
   @PostMapping("/auth/logout")
   public ResponseEntity<Void> logout() {
      return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
   }

   @GetMapping("/auth/me")
   public UserResponse me() {
      return UserResponse.from(getCurrentUserUseCase.execute(new GetCurrentUserQuery(Caller.principal().userId())));
   }

   /**
    * Always 202, whether or not the address has an account.
    */
   @PostMapping("/auth/forgot-password")
   public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
      forgotPasswordUseCase.execute(request.toCommand());
      return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
   }

   @PostMapping("/auth/reset-password")
   public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
      resetPasswordUseCase.execute(request.toCommand());
      return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
   }

   private static <T> ResponseEntity<T> noStore(T body) {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
   }
}
