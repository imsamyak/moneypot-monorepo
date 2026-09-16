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
 * One money movement the round asks the wallet to make. Its id is the reference sent to the wallet, so the wallet runs
 * it once however often it is asked. The pot only moves on when the wallet answers.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "round_transfers",
   indexes = {
      @Index(name = "idx_round_transfers_round", columnList = "round_id")
   }
)
public class RoundTransfer extends DomainEntity {

   @Id
   private UUID id;

   @Column(name = "pot_id", nullable = false, updatable = false)
   private UUID potId;

   @Column(name = "round_id", nullable = false, updatable = false)
   private UUID roundId;

   @Column(name = "user_id", nullable = false, updatable = false)
   private UUID userId;

   @Enumerated(EnumType.STRING)
   @Column(name = "kind", nullable = false, updatable = false)
   private TransferKind kind;

   @Column(name = "amount", nullable = false, updatable = false)
   private long amount;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private TransferStatus status;

   @Column(name = "reason")
   private String reason;

   public static RoundTransfer request(UUID potId, UUID roundId, UUID userId, TransferKind kind, long amount) {
      return new RoundTransfer(UUID.randomUUID(), potId, roundId, userId, kind, amount, TransferStatus.REQUESTED, null);
   }

   public boolean isDone() {
      return status == TransferStatus.DONE;
   }

   public boolean isRequested() {
      return status == TransferStatus.REQUESTED;
   }

   public void markDone() {
      this.status = TransferStatus.DONE;
      this.reason = null;
   }

   public void markRejected(String reason) {
      this.status = TransferStatus.REJECTED;
      this.reason = reason;
   }

   /** Asks again after a rejection, for example once the member has topped up their wallet. */
   public void retry() {
      if (status != TransferStatus.REJECTED) {
         throw BusinessRuleException.of(status == TransferStatus.DONE
               ? "This contribution is already paid" : "This contribution is already being processed");
      }
      this.status = TransferStatus.REQUESTED;
      this.reason = null;
   }

   @Override
   public String getEntityName() {
      return "RoundTransfer";
   }
}
