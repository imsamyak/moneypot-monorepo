package com.moneypot.pot.port.out.notification;

import java.util.UUID;

/**
 * A one-time ticket that lets a browser open the live connection of a pot. The browser cannot send its login token
 * on a WebSocket, so it asks for a ticket first (with the token) and shows the ticket instead.
 */
public interface SocketTicketPort {

   /** A ticket for {@code userId} to watch {@code potId}; it works once and only for a short time. */
   String issue(UUID potId, UUID userId);
}
