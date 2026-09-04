package com.moneypot.inbox;

import com.moneypot.shared.messaging.DomainEvent;

import java.util.Objects;

/**
 * Encapsulates an incoming broker event payload alongside its unique message identifier.
 *
 * @param <E> the domain event type
 */
public record InboxEvent<E extends DomainEvent>(String messageId, E event) {

   public InboxEvent {
      Objects.requireNonNull(messageId, "messageId must not be null");
      Objects.requireNonNull(event, "event must not be null");
   }
}
