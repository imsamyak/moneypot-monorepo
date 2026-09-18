package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.StartPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

public interface StartPotUseCase extends OutboxUseCase<StartPotCmd, PotChangeResult> {
}
