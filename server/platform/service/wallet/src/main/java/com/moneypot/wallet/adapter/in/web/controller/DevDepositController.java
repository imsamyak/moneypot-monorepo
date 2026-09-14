package com.moneypot.wallet.adapter.in.web.controller;

import com.moneypot.security.Caller;
import com.moneypot.security.RequireRole;
import com.moneypot.security.UserRole;
import com.moneypot.wallet.adapter.in.web.controller.dto.request.DepositRequest;
import com.moneypot.wallet.adapter.in.web.controller.dto.response.AccountResponse;
import com.moneypot.wallet.port.in.usecase.DepositUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets a member top up their own wallet. There is no bank behind it, so it only exists when
 * {@code wallet.deposit.enabled=true} (the local profile); production leaves it off.
 */
@RestController
@RequestMapping("/wallet/deposit")
@RequireRole({UserRole.ROLE_MEMBER, UserRole.ROLE_ADMIN})
@ConditionalOnProperty(prefix = "wallet.deposit", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDepositController {

   private final DepositUseCase depositUseCase;

   @PostMapping
   public AccountResponse deposit(@Valid @RequestBody DepositRequest request) {
      return AccountResponse.from(depositUseCase.execute(request.toCommand(Caller.userId())));
   }
}
