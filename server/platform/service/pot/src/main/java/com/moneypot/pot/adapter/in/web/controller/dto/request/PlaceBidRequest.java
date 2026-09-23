package com.moneypot.pot.adapter.in.web.controller.dto.request;

import com.moneypot.pot.port.in.usecase.dto.command.PlaceBidCmd;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** @param amount in paise: what you will accept for the pot. The lowest bid wins. */
public record PlaceBidRequest(
      @Positive(message = "The bid must be greater than zero")
      long amount
) {

   public PlaceBidCmd toCommand(UUID potId, UUID userId) {
      return new PlaceBidCmd(potId, userId, amount);
   }
}
