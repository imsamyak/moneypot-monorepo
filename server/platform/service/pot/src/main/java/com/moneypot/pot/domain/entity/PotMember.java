package com.moneypot.pot.domain.entity;

import com.moneypot.shared.entity.DomainEntity;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A member of a pot. The join order breaks ties and decides who gets a leftover paisa. A member who leaves keeps their
 * row (and their number), so joining again later reuses it.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "pot_members",
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"pot_id", "user_id"}, name = "uc_pot_members_pot_user")
   }
)
public class PotMember extends DomainEntity {

   @Id
   private UUID id;

   @Column(name = "pot_id", nullable = false, updatable = false)
   private UUID potId;

   @Column(name = "user_id", nullable = false, updatable = false)
   private UUID userId;

   @Column(name = "join_order", nullable = false, updatable = false)
   private int joinOrder;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private MemberStatus status;

   /** True once this member has won a round; they cannot win or bid again. */
   @Column(name = "won", nullable = false)
   private boolean won;

   @Column(name = "won_round")
   private Integer wonRound;

   /** A new member, waiting for their contribution to arrive in the pot. */
   public static PotMember join(UUID potId, UUID userId, int joinOrder) {
      return new PotMember(UUID.randomUUID(), potId, userId, joinOrder, MemberStatus.PENDING, false, null);
   }

   public boolean isActive() {
      return status == MemberStatus.ACTIVE;
   }

   public boolean isPending() {
      return status == MemberStatus.PENDING;
   }

   public boolean isLeft() {
      return status == MemberStatus.LEFT;
   }

   /** The contribution arrived: this member counts. */
   public void activate() {
      if (status != MemberStatus.PENDING) {
         throw BusinessRuleException.of("Only a member whose payment is on its way can be activated");
      }
      this.status = MemberStatus.ACTIVE;
   }

   public void leave() {
      this.status = MemberStatus.LEFT;
   }

   /** Joining again after having left. */
   public void rejoin() {
      if (status != MemberStatus.LEFT) {
         throw BusinessRuleException.of("You are already in this pot");
      }
      this.status = MemberStatus.PENDING;
   }

   public void markWon(int round) {
      this.won = true;
      this.wonRound = round;
   }

   @Override
   public String getEntityName() {
      return "PotMember";
   }
}
