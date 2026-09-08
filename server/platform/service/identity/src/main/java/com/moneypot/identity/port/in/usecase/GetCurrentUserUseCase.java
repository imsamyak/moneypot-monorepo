package com.moneypot.identity.port.in.usecase;

import com.moneypot.identity.port.in.usecase.dto.query.GetCurrentUserQuery;
import com.moneypot.identity.port.in.usecase.dto.result.UserResult;
import com.moneypot.shared.usecase.QueryUseCase;

public interface GetCurrentUserUseCase extends QueryUseCase<GetCurrentUserQuery, UserResult> {
}
