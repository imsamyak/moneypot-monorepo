package com.moneypot.contract.pot;

import com.moneypot.shared.messaging.DomainEvent;
import lombok.Builder;

/**
 * Domain events emitted by the Pot boundary. Money amounts are in paise (1/100 of a rupee).
 * <p>
 * {@code TransferRequested} is how the pot asks the wallet to move money; the wallet answers with
 * {@code Wallet.TransferCompleted} or {@code Wallet.TransferRejected} carrying the same {@code reference}.
 */
public sealed interface Pot extends DomainEvent {

   String potId();

   @Override
   default String aggregateId() {
      return potId();
   }

   /** What a transfer is for; it decides the direction: a contribution goes into the pot, the others come out of it. */
   enum Kind {
      CONTRIBUTION,
      PAYOUT,
      DIVIDEND
   }

   /** A new pot exists: the wallet opens the account that holds its money. */
   @Builder
   record Created(String potId, String name, long version) implements Pot {
   }

   /**
    * Move {@code amount} between the pot account and the wallet of {@code userId}.
    *
    * @param reference unique per transfer; the wallet runs a reference only once, so a replay is harmless
    */
   @Builder
   record TransferRequested(String potId, String roundId, String reference, Kind kind, String userId, long amount)
         implements Pot {
   }
}
