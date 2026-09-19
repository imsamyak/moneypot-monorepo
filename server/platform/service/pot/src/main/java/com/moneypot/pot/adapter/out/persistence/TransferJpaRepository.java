package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.RoundTransfer;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TransferJpaRepository extends Repository<RoundTransfer, UUID> {

   Optional<RoundTransfer> findById(UUID id);

   List<RoundTransfer> findByRoundId(UUID roundId);

   RoundTransfer saveAndFlush(RoundTransfer transfer);

   List<RoundTransfer> saveAll(Iterable<RoundTransfer> transfers);
}
