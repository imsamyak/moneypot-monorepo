package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

/** Asks the wallet for the member's contribution (Pot.TransferRequested) through the outbox. */
public interface JoinPotUseCase extends OutboxUseCase<JoinPotCmd, PotChangeResult> {
}
