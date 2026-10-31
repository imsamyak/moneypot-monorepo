package com.moneypot.pot.adapter.in.web.controller;

import com.moneypot.pot.adapter.in.web.controller.dto.request.CreatePotRequest;
import com.moneypot.pot.adapter.in.web.controller.dto.request.JoinByCodeRequest;
import com.moneypot.pot.adapter.in.web.controller.dto.request.JoinPotRequest;
import com.moneypot.pot.adapter.in.web.controller.dto.response.PotListResponse;
import com.moneypot.pot.adapter.in.web.controller.dto.response.PotResponse;
import com.moneypot.pot.adapter.in.web.controller.dto.response.SocketTicketResponse;
import com.moneypot.pot.port.in.usecase.CreateDemoPotUseCase;
import com.moneypot.pot.port.in.usecase.CreatePotUseCase;
import com.moneypot.pot.port.in.usecase.GetPotByInviteUseCase;
import com.moneypot.pot.port.in.usecase.GetPotUseCase;
import com.moneypot.pot.port.in.usecase.IssueSocketTicketUseCase;
import com.moneypot.pot.port.in.usecase.JoinPotUseCase;
import com.moneypot.pot.port.in.usecase.LeavePotUseCase;
import com.moneypot.pot.port.in.usecase.ListPotsUseCase;
import com.moneypot.pot.port.in.usecase.dto.command.CreateDemoPotCmd;
import com.moneypot.pot.port.in.usecase.dto.command.IssueSocketTicketCmd;
import com.moneypot.pot.port.in.usecase.dto.command.JoinPotCmd;
import com.moneypot.pot.port.in.usecase.dto.command.LeavePotCmd;
import com.moneypot.pot.port.in.usecase.dto.query.GetPotByInviteQuery;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Creating, finding, joining and leaving pots. The signed-in member is always the one acting. Pots are by invitation
 * only: the owner shares the invite code, and joining needs it.
 */
@RestController
@RequestMapping("/pots")
@RequireRole({UserRole.ROLE_MEMBER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class PotController {

   private final CreatePotUseCase createPotUseCase;
   private final CreateDemoPotUseCase createDemoPotUseCase;
   private final ListPotsUseCase listPotsUseCase;
   private final GetPotUseCase getPotUseCase;
   private final GetPotByInviteUseCase getPotByInviteUseCase;
   private final JoinPotUseCase joinPotUseCase;
   private final LeavePotUseCase leavePotUseCase;
   private final IssueSocketTicketUseCase issueSocketTicketUseCase;

   /** The owner becomes the first member and pays their contribution at once. */
   @PostMapping
   @ResponseStatus(HttpStatus.CREATED)
   public PotResponse create(@Valid @RequestBody CreatePotRequest request) {
      return PotResponse.from(createPotUseCase.execute(request.toCommand(Caller.userId())).pot());
   }

   /** A practice pot with simulated members and play money, so a new member can try the whole game alone. */
   @PostMapping("/demo")
   @ResponseStatus(HttpStatus.CREATED)
   public PotResponse createDemo() {
      return PotResponse.from(createDemoPotUseCase.execute(new CreateDemoPotCmd(Caller.userId())));
   }

   /** The pots the caller is in. */
   @GetMapping
   public PotListResponse list() {
      return PotListResponse.from(listPotsUseCase.execute(new ListPotsQuery(Caller.userId())));
   }

   @GetMapping("/{potId}")
   public PotResponse get(@PathVariable UUID potId) {
      return PotResponse.from(getPotUseCase.execute(new GetPotQuery(potId, Caller.userId())));
   }

   /** What the invite code opens: the pot, so the person sees what they would pay into before joining. */
   @GetMapping("/invite/{inviteCode}")
   public PotResponse byInvite(@PathVariable String inviteCode) {
      return PotResponse.from(getPotByInviteUseCase.execute(new GetPotByInviteQuery(inviteCode, Caller.userId())));
   }

   /** Joins with just the invite code and pays the contribution; possible until bidding starts. */
   @PostMapping("/join")
   public PotResponse joinByCode(@Valid @RequestBody JoinByCodeRequest request) {
      return PotResponse.from(joinPotUseCase.execute(new JoinPotCmd(null, Caller.userId(), request.inviteCode())).pot());
   }

   /** Joins this pot with its invite code and pays the contribution; possible until bidding starts. */
   @PostMapping("/{potId}/join")
   public PotResponse join(@PathVariable UUID potId, @RequestBody(required = false) JoinPotRequest request) {
      String code = request == null ? null : request.inviteCode();
      return PotResponse.from(joinPotUseCase.execute(new JoinPotCmd(potId, Caller.userId(), code)).pot());
   }

   /** Leaves and gets the contribution back; possible until bidding starts. */
   @PostMapping("/{potId}/leave")
   public PotResponse leave(@PathVariable UUID potId) {
      return PotResponse.from(leavePotUseCase.execute(new LeavePotCmd(potId, Caller.userId())).pot());
   }

   /** A one-time ticket to open the live connection of this pot (members only); see {@code /api/ws/pots}. */
   @PostMapping("/{potId}/socket-ticket")
   public SocketTicketResponse socketTicket(@PathVariable UUID potId) {
      return SocketTicketResponse.from(issueSocketTicketUseCase.execute(new IssueSocketTicketCmd(potId, Caller.userId())));
   }
}
