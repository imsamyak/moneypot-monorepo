package com.moneypot.wallet.adapter.in.web.controller.dto.response;

import com.moneypot.wallet.domain.entity.EntryKind;
import com.moneypot.wallet.domain.entity.EntryType;
import com.moneypot.wallet.port.in.usecase.dto.result.LedgerEntryResult;
import com.moneypot.wallet.port.in.usecase.dto.result.LedgerPageResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** @param nextPage the page to ask for next, or null when this is the last one */
public record LedgerPageResponse(List<Entry> items, Integer nextPage) {

   /** @param amount and balanceAfter in paise */
   public record Entry(UUID id, EntryType type, EntryKind kind, long amount, long balanceAfter, Instant createdAt) {

      static Entry from(LedgerEntryResult entry) {
         return new Entry(entry.id(), entry.type(), entry.kind(), entry.amount(), entry.balanceAfter(), entry.createdAt());
      }
   }

   public static LedgerPageResponse from(LedgerPageResult result) {
      return new LedgerPageResponse(
            result.items().stream().map(Entry::from).toList(),
            result.cursor() == null ? null : Integer.valueOf(result.cursor()));
   }
}
