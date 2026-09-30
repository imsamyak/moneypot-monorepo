package com.moneypot.pot.adapter.in.web.controller;

import com.moneypot.pot.adapter.in.web.controller.dto.request.CreatePotRequest;
import com.moneypot.pot.adapter.in.web.controller.dto.response.PotListResponse;
import com.moneypot.pot.adapter.in.web.controller.dto.response.PotResponse;
import com.moneypot.pot.adapter.in.web.controller.dto.response.SocketTicketResponse;
import com.moneypot.pot.port.in.usecase.CreatePotUseCase;
import com.moneypot.pot.port.in.usecase.GetPotUseCase;
import com.moneypot.pot.port.in.usecase.IssueSocketTicketUseCase;
import com.moneypot.pot.port.in.usecase.JoinPotUseCase;
import com.moneypot.pot.port.in.usecase.ListPotsUseCase;
import com.moneypot.pot.port.in.usecase.StartPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.IssueSocketTicketCmd;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.command.StartPotCmd;
import com.moneypot.pot.port.in.usecase.dto.query.GetPotQuery;
import com.moneypot.pot.port.in.usecase.dto.query.ListPotsQuery;
import com.moneypot.security.Caller;
import com.moneypot.security.RequireRole;
import com.moneypot.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Creating, finding, joining and starting pots. The signed-in member is always the one acting. */
@RestController
@RequestMapping("/pots")
@RequireRole({UserRole.ROLE_MEMBER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class PotController {

   private final CreatePotUseCase createPotUseCase;
   private final ListPotsUseCase listPotsUseCase;
   private final GetPotUseCase getPotUseCase;
   private final JoinPotUseCase joinPotUseCase;
   private final StartPotUseCase startPotUseCase;
   private final IssueSocketTicketUseCase issueSocketTicketUseCase;

   @PostMapping
   @ResponseStatus(HttpStatus.CREATED)
   public PotResponse create(@Valid @RequestBody CreatePotRequest request) {
      return PotResponse.from(createPotUseCase.execute(request.toCommand(Caller.userId())).pot());
   }

   /** {@code scope=open} (default) lists the pots that can still be joined; {@code scope=mine} the member own pots. */
   @GetMapping
   public PotListResponse list(@RequestParam(defaultValue = "open") String scope) {
      return PotListResponse.from(listPotsUseCase.execute(new ListPotsQuery(Caller.userId(), "mine".equals(scope))));
   }

   @GetMapping("/{potId}")
   public PotResponse get(@PathVariable UUID potId) {
      return PotResponse.from(getPotUseCase.execute(new GetPotQuery(potId, Caller.userId())));
   }

   @PostMapping("/{potId}/join")
   public PotResponse join(@PathVariable UUID potId) {
      return PotResponse.from(joinPotUseCase.execute(new JoinPotCmd(potId, Caller.userId())));
   }

   /** A one-time ticket to open the live connection of this pot (members only); see {@code /api/ws/pots}. */
   @PostMapping("/{potId}/socket-ticket")
   public SocketTicketResponse socketTicket(@PathVariable UUID potId) {
      return SocketTicketResponse.from(issueSocketTicketUseCase.execute(new IssueSocketTicketCmd(potId, Caller.userId())));
   }

   @PostMapping("/{potId}/start")
   public PotResponse start(@PathVariable UUID potId) {
      return PotResponse.from(startPotUseCase.execute(new StartPotCmd(potId, Caller.userId())).pot());
   }
}
