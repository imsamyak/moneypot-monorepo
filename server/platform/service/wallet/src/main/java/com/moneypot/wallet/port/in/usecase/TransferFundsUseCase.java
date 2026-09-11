package com.moneypot.wallet.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.TransferFundsCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.TransferResult;

/** Emits Wallet.TransferCompleted or Wallet.TransferRejected through the outbox. */
public interface TransferFundsUseCase extends OutboxUseCase<TransferFundsCmd, TransferResult> {
}
