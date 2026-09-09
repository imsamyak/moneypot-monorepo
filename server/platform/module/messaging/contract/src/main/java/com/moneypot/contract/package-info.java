/**
 * Messaging contracts, event schemas, and payload specifications.
 * <p>
 * Events are nested records of an aggregate interface ({@code Pot.Created}, {@code Wallet.TransferCompleted}); the
 * interface name is the aggregate and the record name is the action. Money is always in paise (a {@code long}).
 * <p>
 * The pot and the wallet talk in request and answer pairs: {@code Pot.TransferRequested} asks the wallet to move money
 * and {@code Wallet.TransferCompleted} / {@code Wallet.TransferRejected} answer it with the same reference. Every
 * consumer is idempotent, so a duplicate or replayed event changes nothing.
 */
package com.moneypot.contract;
