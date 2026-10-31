package com.moneypot.pot.domain.entity;

import com.moneypot.shared.entity.DomainEntity;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A savings pot. People join by invitation and pay their contribution when they join. At the bidding start time joining
 * closes and the members who paid are the group: in each round they pay in, the members who have not won yet bid, and
 * the lowest bid wins; what the winner leaves is shared among the others. Every member wins exactly once, so a pot of N
 * members runs N rounds. Until bidding starts a member can leave and get their money back; after that nobody can.
 * Money is in paise.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "pots",
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"invite_code"}, name = "uc_pots_invite_code")
   }
)
public class Pot extends DomainEntity {

   /** A safety cap, not a rule of the game: every member gets a round, so a huge group would run for very long. */
   public static final int MAX_MEMBERS = 100;
   public static final int MIN_MEMBERS_TO_START = 2;
   public static final long MIN_CONTRIBUTION = 100L;
   public static final long MAX_CONTRIBUTION = 5_000_000L;
   public static final int MIN_BID_WINDOW_SECONDS = 10;
   public static final int MAX_BID_WINDOW_SECONDS = 604_800;
   public static final Duration MAX_TIME_UNTIL_START = Duration.ofDays(90);

   @Id
   private UUID id;

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "owner_id", nullable = false, updatable = false)
   private UUID ownerId;

   /** The members who paid when bidding started; 0 until then. */
   @Column(name = "member_count", nullable = false)
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

   /** The number of the running round; 0 until bidding starts. */
   @Column(name = "current_round", nullable = false)
   private int currentRound;

   /** When joining closes and the first round of bidding begins. */
   @Column(name = "starts_at", nullable = false, updatable = false)
   private Instant startsAt;

   /** The code that finds this pot and lets a member join (the owner does not need it). Only the owner sees it. */
   @Column(name = "invite_code", nullable = false, updatable = false)
   private String inviteCode;

   /** A practice pot: the other members are simulated and no real money moves. */
   @Column(name = "demo", nullable = false, updatable = false, columnDefinition = "boolean not null default false")
   private boolean demo;

   public static Pot create(UUID ownerId, String name, long contribution, int minBidPercent, int bidWindowSeconds,
                            Instant startsAt, Instant now) {
      if (ownerId == null) {
         throw BusinessRuleException.of("A pot needs an owner");
      }
      if (name == null || name.isBlank() || name.length() > 60) {
         throw BusinessRuleException.of("The name must be 1 to 60 characters");
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
      if (startsAt == null || !startsAt.isAfter(now)) {
         throw BusinessRuleException.of("Bidding must start in the future");
      }
      if (startsAt.isAfter(now.plus(MAX_TIME_UNTIL_START))) {
         throw BusinessRuleException.of("Bidding can start at most 90 days from now");
      }
      String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      return new Pot(UUID.randomUUID(), name.trim(), ownerId, 0, contribution, minBidPercent, bidWindowSeconds,
            PotStatus.OPEN, 0, startsAt, inviteCode, false);
   }

   /** A practice pot for someone who wants to try the game alone: it starts at once, with simulated members. */
   public static Pot createDemo(UUID ownerId, long contribution, int minBidPercent, int bidWindowSeconds, Instant now) {
      String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      return new Pot(UUID.randomUUID(), "Demo pot", ownerId, 0, contribution, minBidPercent, bidWindowSeconds,
            PotStatus.OPEN, 0, now, inviteCode, true);
   }

   /** What a round pays out in total with this many members: every member contributes once. */
   public long potAmountFor(int members) {
      return members * contribution;
   }

   /** The lowest bid a member may place with this many members (rounded up, so never below the percentage asked for). */
   public long minBidFor(int members) {
      return Math.ceilDiv(potAmountFor(members) * minBidPercent, 100L);
   }

   public long potAmount() {
      return potAmountFor(memberCount);
   }

   public long minBid() {
      return minBidFor(memberCount);
   }

   public boolean isOpen() {
      return status == PotStatus.OPEN;
   }

   public boolean isActive() {
      return status == PotStatus.ACTIVE;
   }

   /** Joining and leaving are possible only while the pot is open and the bidding start time has not come. */
   public boolean joiningIsOpen(Instant now) {
      return isOpen() && now.isBefore(startsAt);
   }

   public boolean startTimeHasCome(Instant now) {
      return !now.isBefore(startsAt);
   }

   /** Closes joining: these {@code members} (who paid) are the group, and round 1 begins. */
   public void start(int members) {
      if (!isOpen()) {
         throw BusinessRuleException.of("The pot has already started");
      }
      if (members < MIN_MEMBERS_TO_START) {
         throw BusinessRuleException.of("A pot needs at least " + MIN_MEMBERS_TO_START + " members to start");
      }
      status = PotStatus.ACTIVE;
      memberCount = members;
      currentRound = 1;
   }

   /** Too few members joined by the start time. */
   public void cancel() {
      if (!isOpen()) {
         throw BusinessRuleException.of("Only a pot that has not started can be cancelled");
      }
      status = PotStatus.CANCELLED;
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
