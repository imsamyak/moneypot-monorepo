package com.moneypot.wallet.port.in.usecase;

import com.moneypot.shared.usecase.CommandUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.DepositCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;

public interface DepositUseCase extends CommandUseCase<DepositCmd, AccountResult> {
}
