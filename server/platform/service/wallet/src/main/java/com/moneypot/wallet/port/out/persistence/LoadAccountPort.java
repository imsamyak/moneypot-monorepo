package com.moneypot.wallet.port.out.persistence;

import com.moneypot.wallet.domain.entity.Account;

import java.util.Optional;
import java.util.UUID;

public interface LoadAccountPort {

   Optional<Account> loadByOwnerId(UUID ownerId);
}
