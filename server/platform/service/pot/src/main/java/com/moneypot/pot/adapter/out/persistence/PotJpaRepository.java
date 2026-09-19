package com.moneypot.pot.adapter.out.persistence;

import com.moneypot.pot.domain.entity.Pot;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PotJpaRepository extends Repository<Pot, UUID> {

   Optional<Pot> findById(UUID id);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT p FROM Pot p WHERE p.id = :id")
   Optional<Pot> findByIdForUpdate(@Param("id") UUID id);

   @Query("SELECT p FROM Pot p WHERE p.status = com.moneypot.pot.domain.entity.PotStatus.OPEN ORDER BY p.name, p.id")
   List<Pot> findOpen();

   @Query("SELECT p FROM Pot p WHERE p.id IN (SELECT m.potId FROM PotMember m WHERE m.userId = :userId) ORDER BY p.name, p.id")
   List<Pot> findByMember(@Param("userId") UUID userId);

   Pot saveAndFlush(Pot pot);
}
