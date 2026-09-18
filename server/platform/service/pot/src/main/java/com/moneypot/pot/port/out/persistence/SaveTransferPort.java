package com.moneypot.pot.port.out.persistence;

import com.moneypot.pot.domain.entity.RoundTransfer;
import java.util.List;


public interface SaveTransferPort {

   RoundTransfer save(RoundTransfer transfer);

   void saveAll(List<RoundTransfer> transfers);
}
