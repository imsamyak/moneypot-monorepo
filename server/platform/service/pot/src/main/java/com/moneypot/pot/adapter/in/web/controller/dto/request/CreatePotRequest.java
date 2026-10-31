package com.moneypot.pot.adapter.in.web.controller.dto.request;

import com.moneypot.pot.port.in.usecase.dto.command.CreatePotCmd;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Money is in paise: a contribution of Rs 5,000 is 500000. */
public record CreatePotRequest(
      @NotBlank(message = "Give the pot a name")
      @Size(max = 60, message = "The name can be at most 60 characters")
      String name,

      @Min(value = 100, message = "The contribution must be at least Rs 1")
      @Max(value = 5_000_000, message = "The contribution can be at most Rs 50,000")
      long contribution,

      @Min(value = 1, message = "The lowest bid is at least 1 percent of the pot")
      @Max(value = 100, message = "The lowest bid is at most 100 percent of the pot")
      Integer minBidPercent,

      @Min(value = 10, message = "Bidding stays open for at least 10 seconds")
      @Max(value = 604_800, message = "Bidding stays open for at most 7 days")
      Integer bidWindowSeconds,

      @NotNull(message = "Choose when bidding starts")
      Instant startsAt
) {

   /** The share of the pot below which nobody may bid, unless the owner chose another. */
   public static final int DEFAULT_MIN_BID_PERCENT = 60;

   /** How long bidding stays open, unless the owner chose another: one day. */
   public static final int DEFAULT_BID_WINDOW_SECONDS = 86_400;

   public CreatePotCmd toCommand(UUID ownerId) {
      return new CreatePotCmd(ownerId, name, contribution,
            minBidPercent == null ? DEFAULT_MIN_BID_PERCENT : minBidPercent,
            bidWindowSeconds == null ? DEFAULT_BID_WINDOW_SECONDS : bidWindowSeconds,
            startsAt);
   }
}
