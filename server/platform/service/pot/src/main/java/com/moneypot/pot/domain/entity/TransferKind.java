package com.moneypot.pot.domain.entity;

public enum TransferKind {
   /** A member pays their share of the round into the pot. */
   CONTRIBUTION,
   /** The pot pays the winner the winning bid. */
   PAYOUT,
   /** The pot gives a member a share of what the winner left. */
   DIVIDEND
}
