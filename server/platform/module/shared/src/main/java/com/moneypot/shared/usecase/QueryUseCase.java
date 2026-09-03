package com.moneypot.shared.usecase;

import com.moneypot.shared.dto.result.QueryResult;
import org.springframework.validation.annotation.Validated;

@Validated
public interface QueryUseCase<Q, R extends QueryResult> extends UseCase<Q, R> {


}
