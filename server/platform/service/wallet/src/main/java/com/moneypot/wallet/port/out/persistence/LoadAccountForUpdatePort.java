package com.moneypot.wallet.port.out.persistence;

import com.moneypot.wallet.domain.entity.Account;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoadAccountForUpdatePort {

   /** Locks the accounts of these owners until the transaction ends, always in the same order (by account id). */
   List<Account> loadByOwnerIdsForUpdate(Collection<UUID> ownerIds);
}
