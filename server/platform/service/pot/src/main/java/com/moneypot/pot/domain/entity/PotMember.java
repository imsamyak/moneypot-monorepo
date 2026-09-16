package com.moneypot.pot.domain.entity;

import com.moneypot.shared.entity.DomainEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A member of a pot. The join order breaks ties and decides who gets a leftover paisa. */
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

   /** True once this member has won a round; they cannot win or bid again. */
   @Column(name = "won", nullable = false)
   private boolean won;

   @Column(name = "won_round")
   private Integer wonRound;

   public static PotMember join(UUID potId, UUID userId, int joinOrder) {
      return new PotMember(UUID.randomUUID(), potId, userId, joinOrder, false, null);
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
