package com.moneypot.wallet.domain.entity;

/** Why the money moved. */
public enum EntryKind {
   /** Money added by the member (a development top-up). */
   DEPOSIT,
   /** The member paid their share of a round into the pot. */
   CONTRIBUTION,
   /** The pot paid the winning bid to the winner. */
   PAYOUT,
   /** The pot returned a share of what the winner left to a member. */
   DIVIDEND,
   /** The member left before bidding started (or the pot was cancelled) and got their contribution back. */
   REFUND
}
