package com.moneypot.wallet.port.out.persistence;

import com.moneypot.wallet.domain.entity.LedgerEntry;

import java.util.List;

public interface SaveLedgerPort {

   void saveAll(List<LedgerEntry> entries);
}
