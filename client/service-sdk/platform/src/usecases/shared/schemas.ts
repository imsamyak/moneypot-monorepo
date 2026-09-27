import { z } from 'zod';

/** Field rules shared across services, matching the backend's bean validation. */

export const uuid = z.uuid();

/** `@NotBlank` plus `@Size`. Not trimmed, so passwords are sent as typed. */
export const requiredText = (max: number, min = 1) => z.string().regex(/\S/, 'must not be blank').min(min).max(max);

/** Money is a whole number of paise. */
export const paise = (min: number, max: number) => z.number().int().min(min).max(max);

export const page = z.number().int().nonnegative();

/** Page size, `@Min(1) @Max(100)`. */
export const size = z.number().int().min(1).max(100);
