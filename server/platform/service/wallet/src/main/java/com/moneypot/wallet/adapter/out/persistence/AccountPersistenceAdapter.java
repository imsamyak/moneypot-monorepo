package com.moneypot.wallet.adapter.out.persistence;

import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.port.out.persistence.LoadAccountForUpdatePort;
import com.moneypot.wallet.port.out.persistence.LoadAccountPort;
import com.moneypot.wallet.port.out.persistence.SaveAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements LoadAccountPort, LoadAccountForUpdatePort, SaveAccountPort {

   private final AccountJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Account> loadByOwnerId(UUID ownerId) {
      return repository.findByOwnerId(ownerId);
   }

   @Override
   @Transactional
   public List<Account> loadByOwnerIdsForUpdate(Collection<UUID> ownerIds) {
      return repository.findByOwnerIdsForUpdate(ownerIds);
   }

   @Override
   @Transactional
   public Account save(Account account) {
      return repository.saveAndFlush(account);
   }
}
