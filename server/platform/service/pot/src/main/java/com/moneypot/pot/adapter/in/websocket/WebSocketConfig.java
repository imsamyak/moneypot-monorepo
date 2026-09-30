package com.moneypot.pot.adapter.in.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** The live connection of a pot: {@code /api/ws/pots?ticket=...}. */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

   private final PotSocketHandler handler;

   /** The pages that may open the connection (the web app); a browser sends its own address as the origin. */
   @Value("${pot.websocket.allowed-origins:http://localhost:3100}")
   private String[] allowedOrigins;

   @Override
   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(handler, "/api/ws/pots").setAllowedOrigins(allowedOrigins);
   }
}
