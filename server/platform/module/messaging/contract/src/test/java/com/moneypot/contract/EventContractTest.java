package com.moneypot.contract;

import com.moneypot.contract.pot.Pot;
import com.moneypot.contract.wallet.Wallet;
import com.moneypot.shared.messaging.DomainEventRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventContractTest {

   private final DomainEventRegistry registry = new DomainEventRegistry();

   @Test
   @DisplayName("Pot events are named pot.<action> and belong to the pot")
   void potSignatures() {
      Pot.Created created = new Pot.Created("pot-1", "Friends", 0L);
      Pot.TransferRequested requested =
            new Pot.TransferRequested("pot-1", "round-1", "ref-1", Pot.Kind.CONTRIBUTION, "user-1", 500_000L);

      assertEquals("pot-1", created.aggregateId());
      assertEquals("pot-1", requested.aggregateId());
      assertEquals("pot.created", registry.getSignature(created).toString());
      assertEquals("pot.transfer-requested", registry.getSignature(requested).toString());
      assertEquals(Pot.Created.class, registry.getClazz("pot.created"));
      assertEquals(Pot.TransferRequested.class, registry.getClazz("Pot.TransferRequested"));
   }

   @Test
   @DisplayName("Wallet answers are named wallet.<action> and carry the pot id as aggregate id")
   void walletSignatures() {
      Wallet.TransferCompleted completed =
            new Wallet.TransferCompleted("pot-1", "round-1", "ref-1", Pot.Kind.PAYOUT, "user-1", 4_100_000L);
      Wallet.TransferRejected rejected =
            new Wallet.TransferRejected("pot-1", "round-1", "ref-2", Pot.Kind.CONTRIBUTION, "user-2", 500_000L, "Insufficient funds");

      assertEquals("pot-1", completed.aggregateId());
      assertEquals("wallet.transfer-completed", registry.getSignature(completed).toString());
      assertEquals("wallet.transfer-rejected", registry.getSignature(rejected).toString());
      assertEquals(Wallet.TransferRejected.class, registry.getClazz("wallet.transfer-rejected"));
   }
}
