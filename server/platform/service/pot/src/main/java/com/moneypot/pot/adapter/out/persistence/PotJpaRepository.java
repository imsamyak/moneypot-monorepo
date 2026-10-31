package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Pot;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PotJpaRepository extends Repository<Pot, UUID> {

   Optional<Pot> findById(UUID id);

   Optional<Pot> findByInviteCode(String inviteCode);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT p FROM Pot p WHERE p.id = :id")
   Optional<Pot> findByIdForUpdate(@Param("id") UUID id);

   @Query("SELECT p FROM Pot p WHERE p.id IN (SELECT m.potId FROM PotMember m WHERE m.userId = :userId AND m.status <> com.moneypot.pot.domain.entity.MemberStatus.LEFT) ORDER BY p.startsAt DESC, p.id")
   List<Pot> findByMember(@Param("userId") UUID userId);

   @Query("SELECT p FROM Pot p WHERE p.status = com.moneypot.pot.domain.entity.PotStatus.OPEN AND p.startsAt <= :now ORDER BY p.startsAt")
   List<Pot> findOpenStartingBefore(@Param("now") Instant now, Pageable pageable);

   Pot saveAndFlush(Pot pot);
}
