package com.moneypot.wallet.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

/**
 * The outcome of a transfer. A transfer that cannot be made (not enough money) is a normal outcome, not an error: it
 * is reported as rejected so the pot can be told, and nothing is changed.
 *
 * @param reason why it was rejected; null when completed
 */
public record TransferResult(boolean completed, String reason) implements Result.One {

   public static TransferResult done() {
      return new TransferResult(true, null);
   }

   public static TransferResult rejected(String reason) {
      return new TransferResult(false, reason);
   }
}
