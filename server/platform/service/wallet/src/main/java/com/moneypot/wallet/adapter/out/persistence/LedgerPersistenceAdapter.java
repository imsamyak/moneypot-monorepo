package com.moneypot.wallet.adapter.out.persistence;

import com.moneypot.wallet.domain.entity.LedgerEntry;
import com.moneypot.wallet.port.out.persistence.LedgerPage;
import com.moneypot.wallet.port.out.persistence.LoadLedgerPort;
import com.moneypot.wallet.port.out.persistence.SaveLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerPersistenceAdapter implements LoadLedgerPort, SaveLedgerPort {

   private final LedgerJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public boolean existsByReference(String reference) {
      return repository.existsByReference(reference);
   }

   @Override
   @Transactional(readOnly = true)
   public LedgerPage loadPage(UUID accountId, int page, int size) {
      Slice<LedgerEntry> slice = repository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId, PageRequest.of(page, size));
      return new LedgerPage(slice.getContent(), slice.hasNext());
   }

   @Override
   @Transactional
   public void saveAll(List<LedgerEntry> entries) {
      repository.saveAll(entries);
   }
}
