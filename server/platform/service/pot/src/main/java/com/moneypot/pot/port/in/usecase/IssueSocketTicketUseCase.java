package com.moneypot.pot.port.in.usecase;

import com.moneypot.pot.port.in.usecase.dto.command.IssueSocketTicketCmd;
import com.moneypot.pot.port.in.usecase.dto.result.SocketTicketResult;
import com.moneypot.shared.usecase.CommandUseCase;

public interface IssueSocketTicketUseCase extends CommandUseCase<IssueSocketTicketCmd, SocketTicketResult> {
}
