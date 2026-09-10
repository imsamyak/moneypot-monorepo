package com.moneypot.wallet.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One line of an account's history. Lines are only ever added, never changed or deleted, so this entity does not
 * extend DomainEntity (no version, no soft delete). The pair (account, reference) is unique, which is what makes a
 * repeated transfer harmless.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "ledger_entries",
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"account_id", "reference"}, name = "uc_ledger_account_reference")
   },
   indexes = {
      @Index(name = "idx_ledger_account_created", columnList = "account_id, created_at")
   }
)
public class LedgerEntry {

   @Id
   private UUID id;

   @Column(name = "account_id", nullable = false, updatable = false)
   private UUID accountId;

   @Enumerated(EnumType.STRING)
   @Column(name = "type", nullable = false, updatable = false)
   private EntryType type;

   @Enumerated(EnumType.STRING)
   @Column(name = "kind", nullable = false, updatable = false)
   private EntryKind kind;

   /** Always positive; the type says whether it was added or taken. */
   @Column(name = "amount", nullable = false, updatable = false)
   private long amount;

   @Column(name = "balance_after", nullable = false, updatable = false)
   private long balanceAfter;

   @Column(name = "reference", nullable = false, updatable = false)
   private String reference;

   /** The pot or member on the other side of the movement; null for a deposit. */
   @Column(name = "counterparty_id", updatable = false)
   private UUID counterpartyId;

   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt;

   /** Records a change that was just applied to {@code account}, so the balance shown is the one after it. */
   public static LedgerEntry record(Account account, EntryType type, EntryKind kind, long amount, String reference,
                                    UUID counterpartyId, Instant now) {
      return new LedgerEntry(UUID.randomUUID(), account.getId(), type, kind, amount, account.getBalance(), reference,
            counterpartyId, now);
   }
}
