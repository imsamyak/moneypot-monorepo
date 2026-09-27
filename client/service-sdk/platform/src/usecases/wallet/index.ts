import { useCase } from '@moneypot/sdk-core';
import { depositBody, ledgerQuery } from './schemas.js';
import type { AccountResponse, LedgerPageResponse } from './types.js';

export type * from './types.js';

/** Opens the wallet on first use; calling it again returns the same wallet. */
export const openWallet = useCase('OpenWallet')
  .service('wallet')
  .post('/api/wallet')
  .returns<AccountResponse>()
  .authRequired()
  .build();

export const getWallet = useCase('GetWallet')
  .service('wallet')
  .get('/api/wallet')
  .returns<AccountResponse>()
  .authRequired()
  .build();

/** A development top-up: there is no bank behind it. */
export const deposit = useCase('Deposit')
  .service('wallet')
  .post('/api/wallet/deposit')
  .body(depositBody)
  .returns<AccountResponse>()
  .authRequired()
  .build();

/** The wallet history, newest first. */
export const getLedger = useCase('GetLedger')
  .service('wallet')
  .get('/api/wallet/ledger')
  .query(ledgerQuery)
  .returns<LedgerPageResponse>()
  .authRequired()
  .build();

export const walletUseCases = [openWallet, getWallet, deposit, getLedger] as const;
