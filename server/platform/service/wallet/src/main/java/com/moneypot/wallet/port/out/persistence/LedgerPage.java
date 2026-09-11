package com.moneypot.wallet.port.out.persistence;

import com.moneypot.wallet.domain.entity.LedgerEntry;

import java.util.List;

public record LedgerPage(List<LedgerEntry> entries, boolean hasMore) {
}
