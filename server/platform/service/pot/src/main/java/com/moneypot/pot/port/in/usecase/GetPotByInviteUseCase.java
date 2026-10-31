package com.moneypot.pot.port.in.usecase;

import com.moneypot.pot.port.in.usecase.dto.query.GetPotByInviteQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.shared.usecase.QueryUseCase;

public interface GetPotByInviteUseCase extends QueryUseCase<GetPotByInviteQuery, PotResult> {
}
