package com.moneypot.wallet.port.in.usecase;

import com.moneypot.shared.usecase.QueryUseCase;
import com.moneypot.wallet.port.in.usecase.dto.query.GetAccountQuery;
import com.moneypot.wallet.port.in.usecase.dto.result.AccountResult;

public interface GetAccountUseCase extends QueryUseCase<GetAccountQuery, AccountResult> {
}
