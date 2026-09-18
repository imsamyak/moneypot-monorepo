package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.CreatePotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

public interface CreatePotUseCase extends OutboxUseCase<CreatePotCmd, PotChangeResult> {
}
