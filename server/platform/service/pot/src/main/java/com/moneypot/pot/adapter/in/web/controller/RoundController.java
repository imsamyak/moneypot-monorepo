package com.moneypot.pot.adapter.in.web.controller;

import com.moneypot.pot.adapter.in.web.controller.dto.request.PlaceBidRequest;
import com.moneypot.pot.adapter.in.web.controller.dto.response.PotResponse;
import com.moneypot.pot.port.in.usecase.PayContributionUseCase;
import com.moneypot.pot.port.in.usecase.PlaceBidUseCase;
import com.moneypot.pot.port.in.usecase.SettleRoundUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.PayContributionCmd;
import com.moneypot.pot.port.in.usecase.dto.command.SettleRoundCmd;
import com.moneypot.security.Caller;
import com.moneypot.security.RequireRole;
import com.moneypot.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** What happens inside the running round: paying again, bidding, and settling early. */
@RestController
@RequestMapping("/pots/{potId}")
@RequireRole({UserRole.ROLE_MEMBER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class RoundController {

   private final PayContributionUseCase payContributionUseCase;
   private final PlaceBidUseCase placeBidUseCase;
   private final SettleRoundUseCase settleRoundUseCase;

   /** Pays again a contribution that was rejected, for example after topping up the wallet. */
   @PostMapping("/contribution")
   public PotResponse payContribution(@PathVariable UUID potId) {
      return PotResponse.from(payContributionUseCase.execute(new PayContributionCmd(potId, Caller.userId())).pot());
   }

   /** Places a bid, or lowers the one already placed. The lowest bid wins. */
   @PostMapping("/bids")
   public PotResponse bid(@PathVariable UUID potId, @Valid @RequestBody PlaceBidRequest request) {
      return PotResponse.from(placeBidUseCase.execute(request.toCommand(potId, Caller.userId())));
   }

   /** Ends the bidding now (the owner or an admin); otherwise it ends by itself when the time is over. */
   @PostMapping("/settle")
   public PotResponse settle(@PathVariable UUID potId) {
      return PotResponse.from(settleRoundUseCase.execute(new SettleRoundCmd(potId, true)).pot());
   }
}
