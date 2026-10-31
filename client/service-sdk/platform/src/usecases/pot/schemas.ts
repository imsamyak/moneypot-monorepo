import { z } from 'zod';
import { paise, requiredText, uuid } from '../shared/schemas.js';

export const potParams = z.strictObject({ potId: uuid });

/**
 * Money is in paise: a contribution of Rs 5,000 is 500000. The limits match the backend. A pot has no member limit:
 * people join by invitation until `startsAt`, when joining closes and bidding begins.
 */
export const createPotBody = z.strictObject({
  name: requiredText(60),
  contribution: paise(100, 5_000_000),
  /** When joining closes and bidding begins, as an ISO date and time (UTC). */
  startsAt: z.iso.datetime(),
  /** The lowest bid, as a percentage of the pot. The backend uses 60 when it is left out. */
  minBidPercent: z.number().int().min(1).max(100).optional(),
  /** How long bidding stays open, in seconds. The backend uses one day when it is left out. */
  bidWindowSeconds: z.number().int().min(10).max(604_800).optional(),
});

/** The code from the invitation link; the owner does not need one. */
export const joinPotBody = z.strictObject({ inviteCode: requiredText(64).optional() });

export const placeBidBody = z.strictObject({ amount: z.number().int().positive() });

/** Pot id in the path plus the bid in the body. */
export const bidParams = potParams;
