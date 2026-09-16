package com.moneypot.pot.domain.entity;

public enum TransferStatus {
   /** Asked of the wallet; no answer yet. */
   REQUESTED,
   DONE,
   /** The wallet could not do it (for example the balance was too low). */
   REJECTED
}
