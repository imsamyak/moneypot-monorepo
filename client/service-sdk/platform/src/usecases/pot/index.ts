import { useCase } from '@moneypot/sdk-core';
import { createPotBody, joinPotBody, placeBidBody, potParams } from './schemas.js';
import type { PotListResponse, PotResponse, SocketTicketResponse } from './types.js';

export type * from './types.js';

/** The caller becomes the owner and the first member, and pays their contribution at once. */
export const createPot = useCase('CreatePot')
  .service('pot')
  .post('/api/pots')
  .body(createPotBody)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** The pots the caller is in. Pots are by invitation only, so there is no list of other pots. */
export const listPots = useCase('ListPots')
  .service('pot')
  .get('/api/pots')
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

/** Joins with the invite code and pays the contribution. Possible until the pot's start time. */
export const joinPot = useCase('JoinPot')
  .service('pot')
  .post('/api/pots/{potId}/join')
  .params(potParams)
  .body(joinPotBody)
  .returns<PotResponse>()
  .authRequired()
  .build();

/** Leaves and gets the contribution back. Possible until the pot's start time; not for the owner. */
export const leavePot = useCase('LeavePot')
  .service('pot')
  .post('/api/pots/{potId}/leave')
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

/** Members only: a one-time ticket to watch the pot live over a WebSocket (the browser cannot send its token there). */
export const issueSocketTicket = useCase('IssueSocketTicket')
  .service('pot')
  .post('/api/pots/{potId}/socket-ticket')
  .params(potParams)
  .returns<SocketTicketResponse>()
  .authRequired()
  .build();

export const potUseCases = [
  createPot,
  listPots,
  getPot,
  joinPot,
  leavePot,
  payContribution,
  placeBid,
  settleRound,
  issueSocketTicket,
] as const;
