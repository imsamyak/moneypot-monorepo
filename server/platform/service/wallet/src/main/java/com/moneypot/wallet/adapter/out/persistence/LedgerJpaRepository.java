package com.moneypot.wallet.adapter.out.persistence;

import com.moneypot.wallet.domain.entity.LedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

interface LedgerJpaRepository extends Repository<LedgerEntry, UUID> {

   boolean existsByReference(String reference);

   Slice<LedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);

   List<LedgerEntry> saveAll(Iterable<LedgerEntry> entries);
}
