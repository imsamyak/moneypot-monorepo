package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Pot;
import java.util.Optional;
import java.util.UUID;


public interface LoadPotForUpdatePort {

   /** Locks the pot row until the transaction ends, so changes to one pot happen one at a time. */
   Optional<Pot> loadByIdForUpdate(UUID potId);
}
