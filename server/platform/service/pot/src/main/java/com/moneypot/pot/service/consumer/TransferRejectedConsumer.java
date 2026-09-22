package com.moneypot.pot.service.consumer;

import com.moneypot.contract.wallet.Wallet;
import com.moneypot.inbox.InboxEvent;
import com.moneypot.inbox.InboxUseCase;
import com.moneypot.pot.port.in.usecase.RecordTransferResultUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.RecordTransferResultCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** The wallet could not move the money (for example the balance was too low): the transfer is marked rejected. */
@Service
@RequiredArgsConstructor
public class TransferRejectedConsumer implements InboxUseCase<Wallet.TransferRejected> {

   private final RecordTransferResultUseCase recordTransferResultUseCase;

   @Override
   public Class<Wallet.TransferRejected> subscribe() {
      return Wallet.TransferRejected.class;
   }

   @Override
   public Void execute(InboxEvent<Wallet.TransferRejected> cmd) {
      recordTransferResultUseCase.execute(
            new RecordTransferResultCmd(UUID.fromString(cmd.event().reference()), false, cmd.event().reason()));
      return null;
   }
}
