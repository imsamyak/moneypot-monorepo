package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.port.in.usecase.ListPotsUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.ListPotsQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotListResult;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** The pots a member belongs to, or the pots that are still open to join. */
@Service
@Validated
@RequiredArgsConstructor
public class ListPotsService implements ListPotsUseCase {

   private final LoadPotPort loadPotPort;
   private final LoadMemberPort loadMemberPort;

   @Override
   @Transactional(readOnly = true)
   public PotListResult execute(ListPotsQuery query) {
      List<Pot> pots = query.mineOnly() ? loadPotPort.loadByMember(query.userId()) : loadPotPort.loadOpen();
      return new PotListResult(pots.stream()
            .map(pot -> new PotListResult.Summary(pot.getId(), pot.getName(), pot.getStatus(), pot.getMemberCount(),
                  loadMemberPort.loadByPotId(pot.getId()).size(), pot.getContribution(), pot.potAmount(),
                  pot.getCurrentRound()))
            .toList());
   }
}
