package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.command.RegisterMemberCmd;
import com.moneypot.identity.port.in.usecase.dto.result.SessionResult;
import com.moneypot.shared.usecase.CommandUseCase;

/** Self sign-up: always creates a member and signs them in. */
public interface RegisterMemberUseCase extends CommandUseCase<RegisterMemberCmd, SessionResult> {
}
