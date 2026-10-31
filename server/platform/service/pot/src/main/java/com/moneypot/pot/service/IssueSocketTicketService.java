package com.moneypot.pot.service;

import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.IssueSocketTicketUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.IssueSocketTicketCmd;
import com.moneypot.pot.port.in.usecase.dto.result.SocketTicketResult;
import com.moneypot.pot.port.out.notification.SocketTicketPort;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.shared.exception.security.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Only a member of the pot may watch it live. */
@Service
@Validated
@RequiredArgsConstructor
public class IssueSocketTicketService implements IssueSocketTicketUseCase {

   private final LoadPotPort loadPotPort;
   private final LoadMemberPort loadMemberPort;
   private final SocketTicketPort socketTicketPort;

   @Override
   @Transactional(readOnly = true)
   public SocketTicketResult execute(IssueSocketTicketCmd cmd) {
      loadPotPort.loadById(cmd.potId()).orElseThrow(() -> PotNotFoundException.of(cmd.potId()));
      boolean member = loadMemberPort.loadByPotId(cmd.potId()).stream().anyMatch(m -> m.getUserId().equals(cmd.userId()) && !m.isLeft());
      if (!member) {
         throw ForbiddenException.of("Only members of the pot can watch it live");
      }
      return new SocketTicketResult(socketTicketPort.issue(cmd.potId(), cmd.userId()));
   }
}
