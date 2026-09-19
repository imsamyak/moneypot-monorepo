package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Bid;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BidJpaRepository extends Repository<Bid, UUID> {

   List<Bid> findByRoundIdOrderByAmountAscPlacedAtAsc(UUID roundId);

   Optional<Bid> findByRoundIdAndUserId(UUID roundId, UUID userId);

   Bid saveAndFlush(Bid bid);
}
