package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;
import com.moneypot.shared.usecase.CommandUseCase;

/** Creates an account with the given role. Used for administrator-created sellers and for seeding. */
public interface CreateUserUseCase extends CommandUseCase<CreateUserCmd, UserResult> {
}
