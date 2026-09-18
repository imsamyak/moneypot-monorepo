package com.moneypot.pot.port.in.usecase;

import com.moneypot.outbox.OutboxUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.PayContributionCmd;
import com.moneypot.pot.port.in.usecase.dto.result.PotChangeResult;

public interface PayContributionUseCase extends OutboxUseCase<PayContributionCmd, PotChangeResult> {
}
