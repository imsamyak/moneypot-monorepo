package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.pot.port.out.persistence.LoadPotForUpdatePort;
import com.moneypot.pot.port.out.persistence.SavePotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PotPersistenceAdapter implements LoadPotPort, LoadPotForUpdatePort, SavePotPort {

   private final PotJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Pot> loadById(UUID potId) {
      return repository.findById(potId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Pot> loadOpen() {
      return repository.findOpen();
   }

   @Override
   @Transactional(readOnly = true)
   public List<Pot> loadByMember(UUID userId) {
      return repository.findByMember(userId);
   }

   @Override
   @Transactional
   public Optional<Pot> loadByIdForUpdate(UUID potId) {
      return repository.findByIdForUpdate(potId);
   }

   @Override
   @Transactional
   public Pot save(Pot pot) {
      return repository.saveAndFlush(pot);
   }
}
