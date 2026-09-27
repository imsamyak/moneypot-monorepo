import { useCase } from '@moneypot/sdk-core';
import { createPotBody, listPotsQuery, placeBidBody, potParams } from './schemas.js';
import type { PotListResponse, PotResponse } from './types.js';

export type * from './types.js';

/** The caller becomes the owner and the first member. */
export const createPot = useCase('CreatePot')
  .service('pot')
  .post('/api/pots')
  .body(createPotBody)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** `scope: 'open'` (default) lists the pots that can still be joined; `'mine'` the caller's own. */
export const listPots = useCase('ListPots')
  .service('pot')
  .get('/api/pots')
  .query(listPotsQuery)
  .returns<PotListResponse>()
  .authRequired()
  .build();

export const getPot = useCase('GetPot')
  .service('pot')
  .get('/api/pots/{potId}')
  .params(potParams)
  .returns<PotResponse>()
  .authRequired()
  .build();

export const joinPot = useCase('JoinPot')
  .service('pot')
  .post('/api/pots/{potId}/join')
  .params(potParams)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** Owner only, and only when the pot is full. Round 1 opens and every member is asked for their contribution. */
export const startPot = useCase('StartPot')
  .service('pot')
  .post('/api/pots/{potId}/start')
  .params(potParams)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** Asks again for a contribution that was rejected, for example after topping up the wallet. */
export const payContribution = useCase('PayContribution')
  .service('pot')
  .post('/api/pots/{potId}/contribution')
  .params(potParams)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** Places a bid, or lowers the one already placed. The lowest bid wins. */
export const placeBid = useCase('PlaceBid')
  .service('pot')
  .post('/api/pots/{potId}/bids')
  .params(potParams)
  .body(placeBidBody)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** Owner or admin: ends the bidding now. Otherwise it ends by itself when the time is over. */
export const settleRound = useCase('SettleRound')
  .service('pot')
  .post('/api/pots/{potId}/settle')
  .params(potParams)
  .returns<PotResponse>()
  .authRequired()
  .build();

export const potUseCases = [createPot, listPots, getPot, joinPot, startPot, payContribution, placeBid, settleRound] as const;
