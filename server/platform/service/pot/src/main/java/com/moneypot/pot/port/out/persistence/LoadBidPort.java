package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.Bid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface LoadBidPort {

   List<Bid> loadByRoundId(UUID roundId);

   Optional<Bid> loadByRoundIdAndUserId(UUID roundId, UUID userId);
}
