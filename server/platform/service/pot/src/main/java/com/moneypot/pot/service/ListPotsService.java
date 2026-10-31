package com.moneypot.pot.service;

import com.moneypot.pot.domain.entity.Pot;
import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.port.in.usecase.ListPotsUseCase;
import com.moneypot.pot.port.in.usecase.dto.query.ListPotsQuery;
import com.moneypot.pot.port.in.usecase.dto.result.PotListResult;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.LoadPotPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** The pots a member belongs to. Pots are by invitation only, so there is no list of other pots. */
@Service
@Validated
@RequiredArgsConstructor
public class ListPotsService implements ListPotsUseCase {

   private final LoadPotPort loadPotPort;
   private final LoadMemberPort loadMemberPort;

   @Override
   @Transactional(readOnly = true)
   public PotListResult execute(ListPotsQuery query) {
      return new PotListResult(loadPotPort.loadByMember(query.userId()).stream()
            .map(pot -> {
               int members = pot.isOpen()
                     ? (int) loadMemberPort.loadByPotId(pot.getId()).stream().filter(PotMember::isActive).count()
                     : pot.getMemberCount();
               return new PotListResult.Summary(pot.getId(), pot.getName(), pot.getStatus(), members, pot.getContribution(),
                     pot.potAmountFor(members), pot.getCurrentRound(), pot.getStartsAt());
            })
            .toList());
   }
}
