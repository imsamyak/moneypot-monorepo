package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Pot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface LoadPotPort {

   Optional<Pot> loadById(UUID potId);

   /** Pots that have not started, newest first. */
   List<Pot> loadOpen();

   List<Pot> loadByMember(UUID userId);
}
