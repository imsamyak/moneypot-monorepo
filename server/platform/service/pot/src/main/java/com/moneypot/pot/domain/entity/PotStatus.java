package com.moneypot.pot.domain.entity;

public enum PotStatus {
   /** Members are still joining. */
   OPEN,
   /** The rounds are running. */
   ACTIVE,
   /** Every member has won once. */
   COMPLETED
}
