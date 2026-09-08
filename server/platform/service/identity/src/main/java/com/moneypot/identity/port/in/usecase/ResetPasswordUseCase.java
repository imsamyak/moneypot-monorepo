package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.command.ResetPasswordCmd;
import com.moneypot.identity.port.in.usecase.dto.result.Done;
import com.moneypot.shared.usecase.CommandUseCase;

/**
 * Sets a new password for whoever holds a valid reset link. The link works once.
 */
public interface ResetPasswordUseCase extends CommandUseCase<ResetPasswordCmd, Done> {
}
