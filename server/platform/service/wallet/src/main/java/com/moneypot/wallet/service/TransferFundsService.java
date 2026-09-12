package com.moneypot.wallet.service;

import com.moneypot.contract.pot.Pot;
import com.moneypot.contract.wallet.Wallet;
import com.moneypot.shared.messaging.DomainEvent;
import com.moneypot.wallet.domain.entity.Account;
import com.moneypot.wallet.domain.entity.EntryKind;
import com.moneypot.wallet.domain.entity.EntryType;
import com.moneypot.wallet.domain.entity.LedgerEntry;
import com.moneypot.wallet.domain.entity.OwnerType;
import com.moneypot.wallet.port.in.usecase.TransferFundsUseCase;
import com.moneypot.wallet.port.in.usecase.dto.command.TransferFundsCmd;
import com.moneypot.wallet.port.in.usecase.dto.result.TransferResult;
import com.moneypot.wallet.port.out.persistence.LoadAccountForUpdatePort;
import com.moneypot.wallet.port.out.persistence.LoadLedgerPort;
import com.moneypot.wallet.port.out.persistence.SaveAccountPort;
import com.moneypot.wallet.port.out.persistence.SaveLedgerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;

/**
 * Moves money between a member and a pot in one transaction: both balances change and both ledger lines are written,
 * or nothing is. Running the same reference again changes nothing and answers "completed" again, so a retried or
 * replayed request is safe. The member account is opened on the spot if it does not exist yet.
 */
@Service
@Validated
@RequiredArgsConstructor
public class TransferFundsService implements TransferFundsUseCase {

   private final LoadAccountForUpdatePort loadAccountForUpdatePort;
   private final SaveAccountPort saveAccountPort;
   private final LoadLedgerPort loadLedgerPort;
   private final SaveLedgerPort saveLedgerPort;

   @Override
   @Transactional
   public TransferResult execute(TransferFundsCmd cmd) {
      if (loadLedgerPort.existsByReference(cmd.reference())) {
         return TransferResult.done();
      }

      // Both accounts are locked in one query, in a fixed order, so two transfers can never wait on each other.
      List<Account> locked = loadAccountForUpdatePort.loadByOwnerIdsForUpdate(List.of(cmd.userId(), cmd.potId()));
      Account member = find(locked, cmd.userId());
      if (member == null) {
         member = saveAccountPort.save(Account.open(OwnerType.USER, cmd.userId()));
      }
      Account pot = find(locked, cmd.potId());
      if (pot == null) {
         return TransferResult.rejected("The pot has no account");
      }

      boolean intoPot = cmd.kind() == Pot.Kind.CONTRIBUTION;
      Account from = intoPot ? member : pot;
      Account to = intoPot ? pot : member;
      if (from.getBalance() < cmd.amount()) {
         return TransferResult.rejected("Insufficient funds");
      }

      from.debit(cmd.amount());
      to.credit(cmd.amount());
      Account savedFrom = saveAccountPort.save(from);
      Account savedTo = saveAccountPort.save(to);

      Instant now = Instant.now();
      EntryKind kind = EntryKind.valueOf(cmd.kind().name());
      saveLedgerPort.saveAll(List.of(
            LedgerEntry.record(savedFrom, EntryType.DEBIT, kind, cmd.amount(), cmd.reference(), savedTo.getOwnerId(), now),
            LedgerEntry.record(savedTo, EntryType.CREDIT, kind, cmd.amount(), cmd.reference(), savedFrom.getOwnerId(), now)));
      return TransferResult.done();
   }

   @Override
   public List<DomainEvent> buildEvents(TransferFundsCmd cmd, TransferResult result) {
      if (result.completed()) {
         return List.of(new Wallet.TransferCompleted(
               cmd.potId().toString(), cmd.roundId().toString(), cmd.reference(), cmd.kind(), cmd.userId().toString(), cmd.amount()));
      }
      return List.of(new Wallet.TransferRejected(
            cmd.potId().toString(), cmd.roundId().toString(), cmd.reference(), cmd.kind(), cmd.userId().toString(), cmd.amount(),
            result.reason()));
   }

   private static Account find(List<Account> accounts, java.util.UUID ownerId) {
      return accounts.stream().filter(a -> a.getOwnerId().equals(ownerId)).findFirst().orElse(null);
   }
}
