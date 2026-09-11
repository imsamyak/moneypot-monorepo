package com.moneypot.wallet.port.in.usecase;

import com.moneypot.shared.usecase.CommandUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.OpenAccountCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;

public interface OpenAccountUseCase extends CommandUseCase<OpenAccountCmd, AccountResult> {
}
