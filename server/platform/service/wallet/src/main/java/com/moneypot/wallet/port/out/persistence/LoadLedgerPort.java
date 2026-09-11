package com.moneypot.wallet.port.out.persistence;

import java.util.UUID;

public interface LoadLedgerPort {

   /** True when a transfer with this reference already moved money. */
   boolean existsByReference(String reference);

   /** A page of an account history, newest first. */
   LedgerPage loadPage(UUID accountId, int page, int size);
}
