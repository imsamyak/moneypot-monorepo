package com.moneypot.wallet.domain.entity;

import com.moneypot.shared.exception.domain.BusinessRuleException;
import com.moneypot.wallet.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

   private final UUID owner = UUID.randomUUID();

   @Test
   void aNewAccountIsEmpty() {
      Account account = Account.open(OwnerType.USER, owner);

      assertThat(account.getBalance()).isZero();
      assertThat(account.getOwnerId()).isEqualTo(owner);
   }

   @Test
   void creditAndDebitChangeTheBalance() {
      Account account = Account.open(OwnerType.USER, owner).credit(1_000).debit(300);

      assertThat(account.getBalance()).isEqualTo(700);
   }

   @Test
   void theBalanceCanNeverGoBelowZero() {
      Account account = Account.open(OwnerType.USER, owner).credit(100);

      assertThatThrownBy(() -> account.debit(101)).isInstanceOf(InsufficientFundsException.class);
      assertThat(account.getBalance()).isEqualTo(100);
   }

   @Test
   void anAmountMustBePositive() {
      Account account = Account.open(OwnerType.USER, owner);

      assertThatThrownBy(() -> account.credit(0)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> account.debit(-5)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aBalanceThatWouldOverflowIsRefused() {
      Account account = Account.open(OwnerType.USER, owner).credit(Long.MAX_VALUE);

      assertThatThrownBy(() -> account.credit(1)).isInstanceOf(BusinessRuleException.class);
   }
}
