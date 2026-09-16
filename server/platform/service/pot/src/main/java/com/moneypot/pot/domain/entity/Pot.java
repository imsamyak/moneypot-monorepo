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
 * A savings pot: a fixed group of members who each pay the same contribution every round. In a round the members
 * who have not won yet bid for the money and the lowest bid wins; what the winner leaves is shared among the other
 * members. Every member wins exactly once, so a pot of N members runs N rounds. Money is in paise.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pots")
public class Pot extends DomainEntity {

   public static final int MIN_MEMBERS = 2;
   public static final int MAX_MEMBERS = 50;
   public static final long MIN_CONTRIBUTION = 100L;
   public static final long MAX_CONTRIBUTION = 5_000_000L;
   public static final int MIN_BID_WINDOW_SECONDS = 10;
   public static final int MAX_BID_WINDOW_SECONDS = 604_800;

   @Id
   private UUID id;

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "owner_id", nullable = false, updatable = false)
   private UUID ownerId;

   @Column(name = "member_count", nullable = false, updatable = false)
   private int memberCount;

   /** What each member pays every round. */
   @Column(name = "contribution", nullable = false, updatable = false)
   private long contribution;

   /** The lowest bid allowed, as a percentage of the pot. */
   @Column(name = "min_bid_percent", nullable = false, updatable = false)
   private int minBidPercent;

   @Column(name = "bid_window_seconds", nullable = false, updatable = false)
   private int bidWindowSeconds;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private PotStatus status;

   /** The number of the running round; 0 until the pot starts. */
   @Column(name = "current_round", nullable = false)
   private int currentRound;

   public static Pot create(UUID ownerId, String name, int memberCount, long contribution, int minBidPercent,
                            int bidWindowSeconds) {
      if (ownerId == null) {
         throw BusinessRuleException.of("A pot needs an owner");
      }
      if (name == null || name.isBlank() || name.length() > 60) {
         throw BusinessRuleException.of("The name must be 1 to 60 characters");
      }
      if (memberCount < MIN_MEMBERS || memberCount > MAX_MEMBERS) {
         throw BusinessRuleException.of("A pot has " + MIN_MEMBERS + " to " + MAX_MEMBERS + " members");
      }
      if (contribution < MIN_CONTRIBUTION || contribution > MAX_CONTRIBUTION) {
         throw BusinessRuleException.of("The contribution must be between Rs 1 and Rs 50,000");
      }
      if (minBidPercent < 1 || minBidPercent > 100) {
         throw BusinessRuleException.of("The minimum bid is 1 to 100 percent of the pot");
      }
      if (bidWindowSeconds < MIN_BID_WINDOW_SECONDS || bidWindowSeconds > MAX_BID_WINDOW_SECONDS) {
         throw BusinessRuleException.of("Bidding stays open for 10 seconds to 7 days");
      }
      return new Pot(UUID.randomUUID(), name.trim(), ownerId, memberCount, contribution, minBidPercent,
            bidWindowSeconds, PotStatus.OPEN, 0);
   }

   /** What a round pays out in total: every member contributes once. */
   public long potAmount() {
      return memberCount * contribution;
   }

   /** The lowest bid a member may place (rounded up, so the floor is never below the percentage asked for). */
   public long minBid() {
      return Math.ceilDiv(potAmount() * minBidPercent, 100L);
   }

   public boolean isOpen() {
      return status == PotStatus.OPEN;
   }

   public boolean isActive() {
      return status == PotStatus.ACTIVE;
   }

   /** Closes joining and begins round 1. */
   public void start() {
      if (!isOpen()) {
         throw BusinessRuleException.of("The pot has already started");
      }
      status = PotStatus.ACTIVE;
      currentRound = 1;
   }

   public void nextRound() {
      if (!isActive()) {
         throw BusinessRuleException.of("The pot is not running");
      }
      currentRound++;
   }

   public void complete() {
      status = PotStatus.COMPLETED;
   }

   @Override
   public String getEntityName() {
      return "Pot";
   }
}
