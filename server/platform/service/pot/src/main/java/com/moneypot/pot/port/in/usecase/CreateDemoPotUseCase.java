package com.moneypot.pot.port.in.usecase;

import com.moneypot.pot.port.in.usecase.dto.command.CreateDemoPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.shared.usecase.CommandUseCase;

/** No events: a demo pot never touches the wallet. */
public interface CreateDemoPotUseCase extends CommandUseCase<CreateDemoPotCmd, PotResult> {
}
