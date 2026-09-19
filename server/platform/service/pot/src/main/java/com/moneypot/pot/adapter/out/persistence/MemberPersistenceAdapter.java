package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.PotMember;
import com.moneypot.pot.port.out.persistence.LoadMemberPort;
import com.moneypot.pot.port.out.persistence.SaveMemberPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

   private final MemberJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public List<PotMember> loadByPotId(UUID potId) {
      return repository.findByPotIdOrderByJoinOrder(potId);
   }

   @Override
   @Transactional
   public PotMember save(PotMember member) {
      return repository.saveAndFlush(member);
   }
}
