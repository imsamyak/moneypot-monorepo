import { z } from 'zod';
import { page, paise, size } from '../shared/schemas.js';

/** Up to Rs 1,00,000 per top-up. */
export const depositBody = z.strictObject({ amount: paise(1, 10_000_000) });

export const ledgerQuery = z.strictObject({ page: page.optional(), size: size.optional() });
