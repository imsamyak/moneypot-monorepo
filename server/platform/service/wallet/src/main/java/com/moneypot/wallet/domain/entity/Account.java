package com.moneypot.wallet.domain.entity;

import com.moneypot.shared.entity.DomainEntity;
import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.wallet.domain.exception.InsufficientFundsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Holds money for one owner: a member (owner id = user id) or a pot (owner id = pot id). The balance is in paise and
 * can never go below zero; every change to it is written to the ledger by the caller.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "accounts",
   check = {
      @CheckConstraint(constraint = "balance >= 0", name = "chk_accounts_balance")
   },
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"owner_id"}, name = "uc_accounts_owner_id")
   }
)
public class Account extends DomainEntity {

   @Id
   private UUID id;

   @Enumerated(EnumType.STRING)
   @Column(name = "owner_type", nullable = false, updatable = false)
   private OwnerType ownerType;

   @Column(name = "owner_id", nullable = false, updatable = false)
   private UUID ownerId;

   @Column(name = "balance", nullable = false)
   private long balance;

   public static Account open(OwnerType ownerType, UUID ownerId) {
      if (ownerType == null || ownerId == null) {
         throw BusinessRuleException.of("An account needs an owner type and an owner id");
      }
      return new Account(UUID.randomUUID(), ownerType, ownerId, 0);
   }

   public Account credit(long amount) {
      requirePositive(amount);
      try {
         this.balance = Math.addExact(this.balance, amount);
      } catch (ArithmeticException overflow) {
         throw BusinessRuleException.of("The balance would exceed the maximum supported amount");
      }
      return this;
   }

   public Account debit(long amount) {
      requirePositive(amount);
      if (this.balance < amount) {
         throw InsufficientFundsException.of(this.ownerId, amount, this.balance);
      }
      this.balance -= amount;
      return this;
   }

   private static void requirePositive(long amount) {
      if (amount <= 0) {
         throw BusinessRuleException.of("The amount must be greater than zero");
      }
   }

   @Override
   public String getEntityName() {
      return "Account";
   }
}
