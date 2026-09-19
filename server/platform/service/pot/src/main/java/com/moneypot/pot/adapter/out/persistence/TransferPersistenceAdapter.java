package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.RoundTransfer;
import com.moneypot.pot.port.out.persistence.LoadTransferPort;
import com.moneypot.pot.port.out.persistence.SaveTransferPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferPersistenceAdapter implements LoadTransferPort, SaveTransferPort {

   private final TransferJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<RoundTransfer> loadById(UUID transferId) {
      return repository.findById(transferId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<RoundTransfer> loadByRoundId(UUID roundId) {
      return repository.findByRoundId(roundId);
   }

   @Override
   @Transactional
   public RoundTransfer save(RoundTransfer transfer) {
      return repository.saveAndFlush(transfer);
   }

   @Override
   @Transactional
   public void saveAll(List<RoundTransfer> transfers) {
      repository.saveAll(transfers);
   }
}
