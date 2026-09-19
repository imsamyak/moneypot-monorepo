package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Round;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RoundJpaRepository extends Repository<Round, UUID> {

   Optional<Round> findByPotIdAndNumber(UUID potId, int number);

   Optional<Round> findById(UUID id);

   List<Round> findByPotIdOrderByNumber(UUID potId);

   @Query("SELECT r FROM Round r WHERE r.status = com.moneypot.pot.domain.entity.RoundStatus.BIDDING AND r.biddingEndsAt <= :now ORDER BY r.biddingEndsAt")
   List<Round> findBiddingEndedBefore(@Param("now") Instant now, Pageable pageable);

   Round saveAndFlush(Round round);
}
