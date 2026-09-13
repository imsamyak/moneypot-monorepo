package com.moneypot.wallet.service.consumer;

import com.moneypot.contract.pot.Pot;
import com.moneypot.inbox.InboxEvent;
import com.moneypot.inbox.InboxUseCase;
import com.moneypot.wallet.domain.entity.OwnerType;
import com.moneypot.wallet.port.in.usecase.OpenAccountUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.OpenAccountCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Opens the account that holds the money of every new pot. Idempotent. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PotCreatedConsumer implements InboxUseCase<Pot.Created> {

   private final OpenAccountUseCase openAccountUseCase;

   @Override
   public Class<Pot.Created> subscribe() {
      return Pot.Created.class;
   }

   @Override
   public Void execute(InboxEvent<Pot.Created> cmd) {
      openAccountUseCase.execute(new OpenAccountCmd(OwnerType.POT, UUID.fromString(cmd.event().potId())));
      log.info("Opened the account of pot {}", cmd.event().potId());
      return null;
   }
}
