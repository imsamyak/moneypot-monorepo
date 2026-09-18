package com.moneypot.pot.port.in.usecase;

import com.moneypot.shared.usecase.CommandUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.PlaceBidCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;

public interface PlaceBidUseCase extends CommandUseCase<PlaceBidCmd, PotResult> {
}
