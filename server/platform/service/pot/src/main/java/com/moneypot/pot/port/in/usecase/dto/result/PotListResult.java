package com.moneypot.pot.port.in.usecase.dto.result;

import com.moneypot.pot.domain.entity.PotStatus;
import com.moneypot.shared.dto.result.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PotListResult(List<Summary> items) implements Result.Many<PotListResult.Summary> {

   public PotListResult {
      items = items == null ? List.of() : List.copyOf(items);
   }

   public record Summary(UUID id, String name, PotStatus status, int memberCount, long contribution, long potAmount,
                         int currentRound, Instant startsAt) {
   }
}
