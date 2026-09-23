package com.moneypot.pot.service;

import com.moneypot.pot.domain.exception.PotNotFoundException;
import com.moneypot.pot.port.in.usecase.GetPotUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.GetPotQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotResult;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import com.moneypot.pot.service.support.PotAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetPotService implements GetPotUseCase {

   private final LoadPotPort loadPotPort;
   private final PotAssembler potAssembler;

   @Override
   @Transactional(readOnly = true)
   public PotResult execute(GetPotQuery query) {
      return loadPotPort.loadById(query.potId())
            .map(pot -> potAssembler.assemble(pot, query.viewerId()))
            .orElseThrow(() -> PotNotFoundException.of(query.potId()));
   }
}
