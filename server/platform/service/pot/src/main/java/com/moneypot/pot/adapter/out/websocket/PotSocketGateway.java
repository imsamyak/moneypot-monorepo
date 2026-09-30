package com.moneypot.pot.adapter.out.websocket;

import com.moneypot.pot.port.out.notification.NotifyPotChangedPort;
import com.moneypot.pot.port.out.notification.SocketTicketPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live connections of the pots, and the tickets that open them. It lives in memory of this one service instance;
 * with several instances the "changed" message would have to be shared between them (for example through RabbitMQ).
 */
@Slf4j
@Component
public class PotSocketGateway implements NotifyPotChangedPort, SocketTicketPort {

   static final Duration TICKET_LIFETIME = Duration.ofSeconds(30);
   static final String CHANGED = "changed";

   private record Ticket(UUID potId, Instant expiresAt) {
   }

   private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
   private final Map<UUID, Set<WebSocketSession>> watchers = new ConcurrentHashMap<>();

   @Override
   public String issue(UUID potId, UUID userId) {
      Instant now = Instant.now();
      tickets.values().removeIf(ticket -> ticket.expiresAt().isBefore(now));
      String ticket = UUID.randomUUID().toString();
      tickets.put(ticket, new Ticket(potId, now.plus(TICKET_LIFETIME)));
      return ticket;
   }

   /** Uses a ticket up: it opens one connection, to one pot, and then it is gone. */
   public Optional<UUID> redeem(String ticket) {
      Ticket found = ticket == null ? null : tickets.remove(ticket);
      if (found == null || found.expiresAt().isBefore(Instant.now())) {
         return Optional.empty();
      }
      return Optional.of(found.potId());
   }

   public void watch(UUID potId, WebSocketSession session) {
      watchers.computeIfAbsent(potId, id -> ConcurrentHashMap.newKeySet()).add(session);
   }

   public void stopWatching(WebSocketSession session) {
      watchers.values().forEach(sessions -> sessions.remove(session));
   }

   @Override
   public void potChanged(UUID potId) {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
         TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
               broadcast(potId);
            }
         });
      } else {
         broadcast(potId);
      }
   }

   private void broadcast(UUID potId) {
      for (WebSocketSession session : watchers.getOrDefault(potId, Set.of())) {
         try {
            synchronized (session) {
               if (session.isOpen()) {
                  session.sendMessage(new TextMessage(CHANGED));
               }
            }
         } catch (IOException | RuntimeException e) {
            log.debug("Could not tell a watcher of pot {} (it will reconnect): {}", potId, e.getMessage());
         }
      }
   }
}
