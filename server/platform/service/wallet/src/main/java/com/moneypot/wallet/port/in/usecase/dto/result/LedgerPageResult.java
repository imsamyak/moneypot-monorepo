package com.moneypot.wallet.port.in.usecase.dto.result;

import com.moneypot.shared.dto.result.Result;

import java.util.List;

/** @param cursor the next page number as text, or null when this is the last page */
public record LedgerPageResult(List<LedgerEntryResult> items, String cursor) implements Result.Page<LedgerEntryResult> {

   public LedgerPageResult {
      items = items == null ? List.of() : List.copyOf(items);
   }
}
