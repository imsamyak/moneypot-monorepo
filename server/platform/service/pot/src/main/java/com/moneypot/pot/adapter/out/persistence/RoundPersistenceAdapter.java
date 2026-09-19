package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Round;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import com.moneypot.pot.port.out.persistence.LoadRoundPort;
import com.moneypot.pot.port.out.persistence.SaveRoundPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoundPersistenceAdapter implements LoadRoundPort, SaveRoundPort {

   private final RoundJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Round> loadByPotIdAndNumber(UUID potId, int number) {
      return repository.findByPotIdAndNumber(potId, number);
   }

   @Override
   @Transactional(readOnly = true)
   public Optional<Round> loadById(UUID roundId) {
      return repository.findById(roundId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Round> loadByPotId(UUID potId) {
      return repository.findByPotIdOrderByNumber(potId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Round> loadBiddingEndedBefore(Instant now, int limit) {
      return repository.findBiddingEndedBefore(now, PageRequest.of(0, limit));
   }

   @Override
   @Transactional
   public Round save(Round round) {
      return repository.saveAndFlush(round);
   }
}
