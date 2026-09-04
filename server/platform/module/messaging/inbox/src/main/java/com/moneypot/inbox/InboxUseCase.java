package com.moneypot.inbox;

import com.moneypot.shared.messaging.DomainEvent;
import com.moneypot.shared.usecase.UseCase;

/**
 * A use case executed in response to an incoming domain event consumed through the transactional inbox.
 * Implementations declare the domain event class they subscribe to via {@link #subscribe()}.
 *
 * @param <E> the type of domain event subscribed to
 */
public interface InboxUseCase<E extends DomainEvent> extends UseCase<InboxEvent<E>, Void> {


   /**
     * Declares the domain event class this use case subscribes to.
     * Guaranteed at compile-time to match the event type accepted by {@link #execute(InboxEvent)}.
     *
     * @return the subscribed domain event class
     */
   Class<E> subscribe();
}
