package com.moneypot.pot.port.in.usecase;

import com.moneypot.shared.usecase.CommandUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;

public interface JoinPotUseCase extends CommandUseCase<JoinPotCmd, PotResult> {
}
