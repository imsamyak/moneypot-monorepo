package com.moneypot.pot.domain.entity;

public enum PotStatus {
   /** Members are joining (and paying in) until the bidding start time. */
   OPEN,
   /** Bidding has started; the rounds are running and nobody can join or leave. */
   ACTIVE,
   /** Every member has won once. */
   COMPLETED,
   /** Fewer than two members had joined when the bidding start time came, so everyone was refunded. */
   CANCELLED
}
