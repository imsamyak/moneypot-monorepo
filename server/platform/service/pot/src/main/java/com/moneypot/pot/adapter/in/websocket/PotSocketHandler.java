package com.moneypot.pot.adapter.in.websocket;

import com.moneypot.pot.adapter.out.websocket.PotSocketGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * One connection per watching page. It is opened with {@code ?ticket=...}; a missing, used or old ticket is closed
 * at once. The page sends nothing: the server only says "changed".
 */
@Component
@RequiredArgsConstructor
public class PotSocketHandler extends TextWebSocketHandler {

   private final PotSocketGateway gateway;

   @Override
   public void afterConnectionEstablished(WebSocketSession session) throws Exception {
      String ticket = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("ticket");
      UUID potId = gateway.redeem(ticket).orElse(null);
      if (potId == null) {
         session.close(CloseStatus.POLICY_VIOLATION.withReason("A valid ticket is required"));
         return;
      }
      gateway.watch(potId, session);
   }

   @Override
   public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
      gateway.stopWatching(session);
   }
}
