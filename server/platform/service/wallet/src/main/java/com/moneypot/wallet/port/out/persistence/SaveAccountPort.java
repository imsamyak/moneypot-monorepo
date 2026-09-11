package com.moneypot.wallet.port.out.persistence;

import com.moneypot.wallet.domain.entity.Account;

public interface SaveAccountPort {

   Account save(Account account);
}
