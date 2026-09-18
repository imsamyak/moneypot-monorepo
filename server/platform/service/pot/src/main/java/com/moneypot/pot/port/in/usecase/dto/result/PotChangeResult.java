package com.moneypot.pot.port.in.usecase.dto.result;

import com.moneypot.pot.domain.entity.TransferKind;
import com.moneypot.shared.dto.result.Result;

import java.util.List;
import java.util.UUID;

/**
 * The pot after a change, plus the money movements that change asks the wallet to make. Each request becomes a
 * Pot.TransferRequested event in the outbox.
 */
public record PotChangeResult(PotResult pot, List<TransferRequest> requests) implements Result.One {

   public PotChangeResult {
      requests = requests == null ? List.of() : List.copyOf(requests);
   }

   /** @param transferId the reference the wallet runs only once */
   public record TransferRequest(UUID transferId, UUID roundId, UUID userId, TransferKind kind, long amount) {
   }
}
