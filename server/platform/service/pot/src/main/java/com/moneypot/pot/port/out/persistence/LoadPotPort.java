package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Pot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPotPort {

   Optional<Pot> loadById(UUID potId);

   /** The pots this user is in (or is joining). */
   List<Pot> loadByMember(UUID userId);

   /** Pots that have not started whose bidding start time has come, oldest first. */
   List<Pot> loadOpenStartingBefore(Instant now, int limit);
}
