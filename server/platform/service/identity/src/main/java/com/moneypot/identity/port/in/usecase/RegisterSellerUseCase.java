package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.command.RegisterSellerCmd;
import com.moneypot.identity.port.in.usecase.dto.result.SessionResult;
import com.moneypot.shared.usecase.CommandUseCase;

/** Self sign-up for a seller: always creates a seller and signs them in. */
public interface RegisterSellerUseCase extends CommandUseCase<RegisterSellerCmd, SessionResult> {
}
