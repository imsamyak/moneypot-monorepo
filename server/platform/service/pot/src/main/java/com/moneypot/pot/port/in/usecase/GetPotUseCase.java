package com.moneypot.pot.port.in.usecase;

import com.moneypot.shared.usecase.QueryUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.GetPotQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;

public interface GetPotUseCase extends QueryUseCase<GetPotQuery, PotResult> {
}
