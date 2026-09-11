package com.moneypot.wallet.adapter.out.persistence;

import com.moneypot.wallet.domain.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AccountJpaRepository extends Repository<Account, UUID> {

   Optional<Account> findByOwnerId(UUID ownerId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT a FROM Account a WHERE a.ownerId IN :ownerIds ORDER BY a.id")
   List<Account> findByOwnerIdsForUpdate(@Param("ownerIds") Collection<UUID> ownerIds);

   Account saveAndFlush(Account account);
}
