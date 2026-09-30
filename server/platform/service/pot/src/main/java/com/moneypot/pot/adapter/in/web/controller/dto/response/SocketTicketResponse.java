package com.moneypot.pot.adapter.in.web.controller.dto.response;

import com.moneypot.pot.port.in.usecase.dto.result.SocketTicketResult;

/** Open {@code /api/ws/pots?ticket=<ticket>} within 30 seconds. */
public record SocketTicketResponse(String ticket) {

   public static SocketTicketResponse from(SocketTicketResult result) {
      return new SocketTicketResponse(result.ticket());
   }
}
