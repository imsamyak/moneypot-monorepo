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

/** One month of a pot: collect the contributions, bid, then pay the winner and share the rest. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "pot_rounds",
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"pot_id", "number"}, name = "uc_pot_rounds_pot_number")
   },
   indexes = {
      @Index(name = "idx_pot_rounds_due", columnList = "status, bidding_ends_at")
   }
)
public class Round extends DomainEntity {

   @Id
   private UUID id;

   @Column(name = "pot_id", nullable = false, updatable = false)
   private UUID potId;

   @Column(name = "number", nullable = false, updatable = false)
   private int number;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private RoundStatus status;

   @Column(name = "bidding_ends_at")
   private Instant biddingEndsAt;

   @Column(name = "winner_id")
   private UUID winnerId;

   /** What the winner receives. */
   @Column(name = "payout")
   private Long payout;

   public static Round open(UUID potId, int number) {
      return new Round(UUID.randomUUID(), potId, number, RoundStatus.COLLECTING, null, null, null);
   }

   public void startBidding(Instant endsAt) {
      require(RoundStatus.COLLECTING, "Bidding can only start once every contribution is paid");
      this.status = RoundStatus.BIDDING;
      this.biddingEndsAt = endsAt;
   }

   public boolean biddingIsOpen(Instant now) {
      return status == RoundStatus.BIDDING && now.isBefore(biddingEndsAt);
   }

   public boolean biddingHasEnded(Instant now) {
      return status == RoundStatus.BIDDING && !now.isBefore(biddingEndsAt);
   }

   /** The winner is decided: from here the payout and the dividends are paid out of the pot. */
   public void startSettling(UUID winnerId, long payout) {
      if (status != RoundStatus.COLLECTING && status != RoundStatus.BIDDING) {
         throw BusinessRuleException.of("The round is already settling");
      }
      this.status = RoundStatus.SETTLING;
      this.winnerId = winnerId;
      this.payout = payout;
   }

   public void settled() {
      require(RoundStatus.SETTLING, "The round is not being settled");
      this.status = RoundStatus.SETTLED;
   }

   private void require(RoundStatus expected, String message) {
      if (status != expected) {
         throw BusinessRuleException.of(message);
      }
   }

   @Override
   public String getEntityName() {
      return "Round";
   }
}
