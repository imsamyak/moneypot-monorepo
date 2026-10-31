package com.moneypot.pot.service;

import com.moneypot.pot.domain.exception.InviteNotFoundException;
import com.moneypot.pot.port.in.usecase.GetPotByInviteUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.GetPotByInviteQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.pot.service.support.PotAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Shows the pot behind an invite code, so the person can see what they would be paying into before they join. */
@Service
@Validated
@RequiredArgsConstructor
public class GetPotByInviteService implements GetPotByInviteUseCase {

   private final LoadPotPort loadPotPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional(readOnly = true)
   public PotResult execute(GetPotByInviteQuery query) {
      return loadPotPort.loadByInviteCode(query.inviteCode())
            .map(pot -> potAssembler.assemble(pot, query.viewerId()))
            .orElseThrow(InviteNotFoundException::of);
   }
}
