package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Bid;
import com.moneypot.pot.port.out.persistence.LoadBidPort;
import com.moneypot.pot.port.out.persistence.SaveBidPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BidPersistenceAdapter implements LoadBidPort, SaveBidPort {

   private final BidJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public List<Bid> loadByRoundId(UUID roundId) {
      return repository.findByRoundIdOrderByAmountAscPlacedAtAsc(roundId);
   }

   @Override
   @Transactional(readOnly = true)
   public Optional<Bid> loadByRoundIdAndUserId(UUID roundId, UUID userId) {
      return repository.findByRoundIdAndUserId(roundId, userId);
   }

   @Override
   @Transactional
   public Bid save(Bid bid) {
      return repository.saveAndFlush(bid);
   }
}
