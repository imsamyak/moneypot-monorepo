package com.moneypot.pot.service.support;

import com.moneypot.contract.pot.Pot;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;
import com.moneypot.shared.messaging.DomainEvent;

import java.util.List;

/** Turns the transfers a use case asked for into the events that carry them to the wallet. */
public final class TransferEvents {

   private TransferEvents() {
   }

   public static List<DomainEvent> of(PotChangeResult result) {
      if (result == null || result.pot() == null) {
         return List.of();
      }
      String potId = result.pot().id().toString();
      return result.requests().stream()
            .map(request -> (DomainEvent) new Pot.TransferRequested(
                  potId,
                  request.roundId().toString(),
                  request.transferId().toString(),
                  Pot.Kind.valueOf(request.kind().name()),
                  request.userId().toString(),
                  request.amount()))
            .toList();
   }
}
