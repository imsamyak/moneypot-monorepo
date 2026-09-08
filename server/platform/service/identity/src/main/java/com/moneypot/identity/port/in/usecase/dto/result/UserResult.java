package com.moneypot.identity.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

public record UserResult(String id, String email, String role, String sellerId) implements Result.One {
}
