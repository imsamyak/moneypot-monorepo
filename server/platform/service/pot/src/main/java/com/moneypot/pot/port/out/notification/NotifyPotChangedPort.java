package com.moneypot.pot.port.out.notification;

import java.util.UUID;

/**
 * Tells everyone watching a pot that it changed (a bid, a payment, a new round), so their screens can refresh at once.
 * The message carries no data: a watcher asks for the pot again, which keeps the bids private to the members.
 */
public interface NotifyPotChangedPort {

   /** Sent once the change is committed; when it is rolled back nothing is sent. */
   void potChanged(UUID potId);
}
