package com.moneypot.wallet.adapter.in.web.controller;

import com.moneypot.security.Caller;
import com.moneypot.security.RequireRole;
import com.moneypot.security.UserRole;
import com.moneypot.wallet.adapter.in.web.controller.dto.response.AccountResponse;
import com.moneypot.wallet.adapter.in.web.controller.dto.response.LedgerPageResponse;
import com.moneypot.wallet.domain.entity.OwnerType;
import com.moneypot.wallet.port.in.usecase.GetAccountUseCase;
import com.moneypot.wallet.port.in.usecase.GetLedgerUseCase;
import com.moneypot.wallet.port.in.usecase.OpenAccountUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.OpenAccountCmd;
import com.moneypot.wallet.port.in.usecase.dto.query.GetAccountQuery;
import com.moneypot.wallet.port.in.usecase.dto.query.GetLedgerQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The wallet of the signed-in member. A member only ever sees their own account. */
@RestController
@RequestMapping("/wallet")
@RequireRole({UserRole.ROLE_MEMBER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class WalletController {

   private final OpenAccountUseCase openAccountUseCase;
   private final GetAccountUseCase getAccountUseCase;
   private final GetLedgerUseCase getLedgerUseCase;

   /** Opens the wallet on first use; calling it again returns the same wallet. */
   @PostMapping
   public AccountResponse open() {
      return AccountResponse.from(openAccountUseCase.execute(new OpenAccountCmd(OwnerType.USER, Caller.userId())));
   }

   @GetMapping
   public AccountResponse get() {
      return AccountResponse.from(getAccountUseCase.execute(new GetAccountQuery(Caller.userId())));
   }

   @GetMapping("/ledger")
   public LedgerPageResponse ledger(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
      return LedgerPageResponse.from(getLedgerUseCase.execute(new GetLedgerQuery(Caller.userId(), page, size)));
   }
}
