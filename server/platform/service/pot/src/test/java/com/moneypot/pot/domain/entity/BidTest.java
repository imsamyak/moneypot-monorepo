package com.moneypot.pot.domain.entity;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BidTest {

   private final Instant now = Instant.parse("2026-09-20T10:00:00Z");

   @Test
   void aBidCanOnlyBeLoweredAndLoweringMovesItsTimeStamp() {
      Bid bid = Bid.place(UUID.randomUUID(), UUID.randomUUID(), 4_000, now);

      bid.lowerTo(3_500, now.plusSeconds(60));

      assertThat(bid.getAmount()).isEqualTo(3_500);
      assertThat(bid.getPlacedAt()).isEqualTo(now.plusSeconds(60));
      assertThatThrownBy(() -> bid.lowerTo(3_500, now.plusSeconds(61))).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> bid.lowerTo(3_600, now.plusSeconds(61))).isInstanceOf(BusinessRuleException.class);
   }
}
