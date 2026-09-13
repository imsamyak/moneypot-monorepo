package com.moneypot.wallet.service.consumer;

import com.moneypot.contract.pot.Pot;
import com.moneypot.inbox.InboxEvent;
import com.moneypot.inbox.InboxUseCase;
import com.moneypot.wallet.port.in.usecase.TransferFundsUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.TransferFundsCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Makes the transfer a pot asked for. The answer (completed or rejected) goes back through the outbox. */
@Service
@RequiredArgsConstructor
public class TransferRequestedConsumer implements InboxUseCase<Pot.TransferRequested> {

   private final TransferFundsUseCase transferFundsUseCase;

   @Override
   public Class<Pot.TransferRequested> subscribe() {
      return Pot.TransferRequested.class;
   }

   @Override
   public Void execute(InboxEvent<Pot.TransferRequested> cmd) {
      Pot.TransferRequested event = cmd.event();
      transferFundsUseCase.execute(new TransferFundsCmd(
            event.reference(),
            event.kind(),
            UUID.fromString(event.potId()),
            UUID.fromString(event.roundId()),
            UUID.fromString(event.userId()),
            event.amount()));
      return null;
   }
}
