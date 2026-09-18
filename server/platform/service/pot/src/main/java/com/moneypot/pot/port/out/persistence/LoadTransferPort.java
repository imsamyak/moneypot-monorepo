package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.RoundTransfer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface LoadTransferPort {

   Optional<RoundTransfer> loadById(UUID transferId);

   List<RoundTransfer> loadByRoundId(UUID roundId);
}
