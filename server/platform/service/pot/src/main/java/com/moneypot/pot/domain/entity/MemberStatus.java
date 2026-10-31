package com.moneypot.pot.domain.entity;

public enum MemberStatus {
   /** Asked to join; the contribution is on its way from the wallet. */
   PENDING,
   /** The contribution is in the pot: this member counts. */
   ACTIVE,
   /** Left before bidding started (or the payment did not go through). */
   LEFT
}
