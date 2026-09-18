package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.SettleRoundCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

public interface SettleRoundUseCase extends OutboxUseCase<SettleRoundCmd, PotChangeResult> {
}
