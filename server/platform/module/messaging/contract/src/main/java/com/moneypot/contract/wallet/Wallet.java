package com.moneypot.contract.wallet;

import com.moneypot.contract.pot.Pot;
import com.moneypot.shared.messaging.DomainEvent;
import lombok.Builder;

/**
 * Domain events emitted by the Wallet boundary: the answer to a {@code Pot.TransferRequested}. They belong to the pot
 * (the aggregate id is the pot id) so that a pot's events stay in order. Money amounts are in paise.
 */
public sealed interface Wallet extends DomainEvent {

   String potId();

   @Override
   default String aggregateId() {
      return potId();
   }

   /** The money moved. */
   @Builder
   record TransferCompleted(String potId, String roundId, String reference, Pot.Kind kind, String userId, long amount)
         implements Wallet {
   }

   /** The money did not move, for example because the member's balance was too low. Nothing was changed. */
   @Builder
   record TransferRejected(String potId, String roundId, String reference, Pot.Kind kind, String userId, long amount,
                           String reason) implements Wallet {
   }
}
