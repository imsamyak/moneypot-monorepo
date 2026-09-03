package com.moneypot.shared.usecase;

import com.moneypot.shared.dto.result.CommandResult;
import org.springframework.validation.annotation.Validated;

@Validated
public interface CommandUseCase<C, R extends CommandResult> extends UseCase<C, R> {


}
