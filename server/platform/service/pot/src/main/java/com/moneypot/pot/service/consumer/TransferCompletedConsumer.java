package com.moneypot.pot.service.consumer;

import com.moneypot.contract.wallet.Wallet;
import com.moneypot.inbox.InboxEvent;
import com.moneypot.inbox.InboxUseCase;
import com.moneypot.pot.port.in.usecase.RecordTransferResultUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.RecordTransferResultCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** The wallet moved the money: the round can go on. */
@Service
@RequiredArgsConstructor
public class TransferCompletedConsumer implements InboxUseCase<Wallet.TransferCompleted> {

   private final RecordTransferResultUseCase recordTransferResultUseCase;

   @Override
   public Class<Wallet.TransferCompleted> subscribe() {
      return Wallet.TransferCompleted.class;
   }

   @Override
   public Void execute(InboxEvent<Wallet.TransferCompleted> cmd) {
      recordTransferResultUseCase.execute(new RecordTransferResultCmd(UUID.fromString(cmd.event().reference()), true, null));
      return null;
   }
}
