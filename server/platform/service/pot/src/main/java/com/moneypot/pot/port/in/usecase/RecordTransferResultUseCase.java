package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.RecordTransferResultCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

public interface RecordTransferResultUseCase extends OutboxUseCase<RecordTransferResultCmd, PotChangeResult> {
}
