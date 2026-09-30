package com.moneypot.pot.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

public record SocketTicketResult(String ticket) implements Result.One {
}
