package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Round;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface LoadRoundPort {

   Optional<Round> loadByPotIdAndNumber(UUID potId, int number);

   Optional<Round> loadById(UUID roundId);

   /** Every round of the pot, oldest first. */
   List<Round> loadByPotId(UUID potId);

   /** Rounds in bidding whose time is over, oldest first. */
   List<Round> loadBiddingEndedBefore(Instant now, int limit);
}
