package com.moneypot.pot.adapter.in.web.controller.dto.response;

import com.moneypot.pot.domain.entity.PotStatus;
import com.moneypot.pot.port.in.usecase.dto.result.PotListResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PotListResponse(List<Summary> items) {

   public record Summary(UUID id, String name, PotStatus status, int memberCount, long contribution, long potAmount,
                         int currentRound, Instant startsAt) {
   }

   public static PotListResponse from(PotListResult result) {
      return new PotListResponse(result.items().stream()
            .map(s -> new Summary(s.id(), s.name(), s.status(), s.memberCount(), s.contribution(), s.potAmount(),
                  s.currentRound(), s.startsAt()))
            .toList());
   }
}
