package com.moneypot.pot.domain.entity;

public enum RoundStatus {
   /** Waiting for every member to pay their contribution into the pot. */
   COLLECTING,
   /** The pot is full; members who have not won yet bid, and the lowest bid wins. */
   BIDDING,
   /** The winner is decided; the payout and the dividends are being paid out of the pot. */
   SETTLING,
   /** Everything is paid; the next round can start. */
   SETTLED
}
