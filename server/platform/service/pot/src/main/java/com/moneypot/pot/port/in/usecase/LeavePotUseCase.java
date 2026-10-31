package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.LeavePotCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

/** Asks the wallet to give the member their contribution back (Pot.TransferRequested, kind REFUND) through the outbox. */
public interface LeavePotUseCase extends OutboxUseCase<LeavePotCmd, PotChangeResult> {
}
