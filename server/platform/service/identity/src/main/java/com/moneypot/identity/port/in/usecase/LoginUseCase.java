package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.command.LoginCmd;
import com.moneypot.identity.port.in.usecase.dto.result.SessionResult;
import com.moneypot.shared.usecase.CommandUseCase;

public interface LoginUseCase extends CommandUseCase<LoginCmd, SessionResult> {
}
