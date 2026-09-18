package com.moneypot.pot.port.in.usecase;

import com.moneypot.shared.usecase.QueryUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.ListPotsQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotListResult;

public interface ListPotsUseCase extends QueryUseCase<ListPotsQuery, PotListResult> {
}
