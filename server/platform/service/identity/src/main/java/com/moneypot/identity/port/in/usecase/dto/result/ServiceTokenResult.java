package com.moneypot.identity.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

public record ServiceTokenResult(String accessToken, long expiresIn) implements Result.One {
}
