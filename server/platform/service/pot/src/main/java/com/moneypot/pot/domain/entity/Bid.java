package com.moneypot.pot.domain.entity;

import com.moneypot.shared.entity.DomainEntity;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * What a member is willing to take for the pot this round. The lowest bid wins, so a member can only lower their own
 * bid. Lowering it moves the time stamp, which is what breaks a tie: the bid placed first at an amount wins.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "bids",
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"round_id", "user_id"}, name = "uc_bids_round_user")
   }
)
public class Bid extends DomainEntity {

   @Id
   private UUID id;

   @Column(name = "round_id", nullable = false, updatable = false)
   private UUID roundId;

   @Column(name = "user_id", nullable = false, updatable = false)
   private UUID userId;

   @Column(name = "amount", nullable = false)
   private long amount;

   @Column(name = "placed_at", nullable = false)
   private Instant placedAt;

   public static Bid place(UUID roundId, UUID userId, long amount, Instant now) {
      return new Bid(UUID.randomUUID(), roundId, userId, amount, now);
   }

   public void lowerTo(long newAmount, Instant now) {
      if (newAmount >= amount) {
         throw BusinessRuleException.of("A bid can only be lowered: your bid is " + amount + " paise");
      }
      this.amount = newAmount;
      this.placedAt = now;
   }

   @Override
   public String getEntityName() {
      return "Bid";
   }
}
