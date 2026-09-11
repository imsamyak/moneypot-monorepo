package com.moneypot.wallet.port.in.usecase;

import com.moneypot.shared.usecase.QueryUseCase;
import com.moneypot.wallet.port.in.usecase.dto.query.GetLedgerQuery;
import com.moneypot.wallet.port.in.usecase.dto.result.LedgerPageResult;

public interface GetLedgerUseCase extends QueryUseCase<GetLedgerQuery, LedgerPageResult> {
}
