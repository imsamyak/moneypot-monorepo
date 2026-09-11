package com.moneypot.wallet.port.in.usecase.dto.result;

import com.moneypot.wallet.domain.entity.EntryKind;
import com.moneypot.wallet.domain.entity.EntryType;
import com.moneypot.wallet.domain.entity.LedgerEntry;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResult(UUID id, EntryType type, EntryKind kind, long amount, long balanceAfter,
                                String reference, UUID counterpartyId, Instant createdAt) {

   public static LedgerEntryResult from(LedgerEntry entry) {
      return new LedgerEntryResult(entry.getId(), entry.getType(), entry.getKind(), entry.getAmount(),
            entry.getBalanceAfter(), entry.getReference(), entry.getCounterpartyId(), entry.getCreatedAt());
   }
}
