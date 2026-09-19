package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.PotMember;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MemberJpaRepository extends Repository<PotMember, UUID> {

   List<PotMember> findByPotIdOrderByJoinOrder(UUID potId);

   PotMember saveAndFlush(PotMember member);
}
