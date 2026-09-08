package com.moneypot.identity.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

/**
 * The result of a command that has nothing to hand back.
 */
public record Done() implements Result.None {

   public static final Done INSTANCE = new Done();
}
