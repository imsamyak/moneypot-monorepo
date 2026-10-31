'use server';

import { SdkHttpError } from '@moneypot/platform-sdk';
import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { errorMessage } from './errors';
import { rupeesToPaise } from './format';
import { api } from './sdk';
import { requireUser } from './session';

export interface FormState {
  error?: string;
  message?: string;
}

/** Only same-site paths, so `?next=` cannot send the member elsewhere. */
function safeNext(value: FormDataEntryValue | null): string {
  const next = typeof value === 'string' ? value : '';
  return next.startsWith('/') && !next.startsWith('//') ? next : '/pots';
}

function credentials(formData: FormData) {
  return { email: String(formData.get('email') ?? ''), password: String(formData.get('password') ?? '') };
}

export async function login(_: FormState, formData: FormData): Promise<FormState> {
  try {
    await api().auth.login(credentials(formData));
  } catch (error) {
    return { error: errorMessage(error) };
  }
  redirect(safeNext(formData.get('next')));
}

export async function register(_: FormState, formData: FormData): Promise<FormState> {
  try {
    await api().auth.register(credentials(formData));
  } catch (error) {
    if (error instanceof SdkHttpError && error.status === 409) {
      return { error: 'An account with this email already exists.' };
    }
    return { error: errorMessage(error) };
  }
  redirect(safeNext(formData.get('next')));
}

export async function logout() {
  await api().auth.logout();
  redirect('/');
}

/** A development top-up: there is no bank behind it. */
export async function deposit(_: FormState, formData: FormData): Promise<FormState> {
  await requireUser('/wallet');
  const amount = rupeesToPaise(formData.get('amount'));
  if (!(amount > 0)) return { error: 'Enter an amount in rupees.' };
  try {
    await api().wallet.deposit({ amount });
  } catch (error) {
    return { error: errorMessage(error) };
  }
  revalidatePath('/wallet');
  return { message: 'Added to your wallet.' };
}

/** Creates a pot. The form sends the start time already as an ISO date (the browser knows the member's time zone). */
export async function createPot(_: FormState, formData: FormData): Promise<FormState> {
  await requireUser('/pots/new');
  const startsAt = String(formData.get('startsAt') ?? '');
  if (!startsAt || Number.isNaN(Date.parse(startsAt))) return { error: 'Choose when bidding starts.' };
  if (Date.parse(startsAt) <= Date.now()) return { error: 'Bidding must start in the future.' };
  const bidWindowSeconds = Math.round(Number(formData.get('bidHours') || 0) * 3600 + Number(formData.get('bidMinutes') || 0) * 60);
  if (!(bidWindowSeconds >= 60)) return { error: 'Bidding must stay open for at least 1 minute.' };
  if (bidWindowSeconds > 7 * 86400) return { error: 'Bidding can stay open for 7 days (168 hours) at most.' };
  let potId: string;
  try {
    const pot = await api().pots.createPot({
      name: String(formData.get('name') ?? ''),
      contribution: rupeesToPaise(formData.get('contribution')),
      minBidPercent: Number(formData.get('minBidPercent')),
      bidWindowSeconds,
      startsAt: new Date(startsAt).toISOString(),
    });
    potId = pot.id;
  } catch (error) {
    return { error: errorMessage(error) };
  }
  redirect(`/pots/${potId}`);
}

/** Starts a practice pot with simulated members and play money, so a new member can try the whole game alone. */
export async function createDemoPot() {
  await requireUser('/pots');
  let potId: string;
  try {
    potId = (await api().pots.createDemoPot()).id;
  } catch (error) {
    redirect(`/pots?error=${encodeURIComponent(errorMessage(error))}`);
  }
  redirect(`/pots/${potId}`);
}

/** Runs one action on a pot, then shows the pot again (with the error, if there was one). */
async function onPot(formData: FormData, run: (potId: string) => Promise<unknown>) {
  const potId = String(formData.get('potId'));
  const here = `/pots/${potId}`;
  await requireUser(here);
  try {
    await run(potId);
  } catch (error) {
    redirect(`${here}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath(here);
  redirect(here);
}

/** Joins with just the invite code; the contribution is taken from the wallet. */
export async function joinByCode(formData: FormData) {
  const inviteCode = String(formData.get('inviteCode') ?? '');
  const here = `/join/${inviteCode}`;
  await requireUser(here);
  let potId: string;
  try {
    potId = (await api().pots.joinPotByCode({ inviteCode })).id;
  } catch (error) {
    redirect(`${here}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath('/pots');
  redirect(`/pots/${potId}`);
}

/** Leaves before bidding starts and gets the contribution back. */
export async function leavePot(formData: FormData) {
  await onPot(formData, (potId) => api().pots.leavePot({ potId }));
}

export async function payContribution(formData: FormData) {
  await onPot(formData, (potId) => api().pots.payContribution({ potId }));
}

export async function placeBid(formData: FormData) {
  const amount = rupeesToPaise(formData.get('amount'));
  await onPot(formData, (potId) => {
    if (!(amount > 0)) throw new Error('Enter your bid in rupees.');
    return api().pots.placeBid({ potId, amount });
  });
}

export async function settleRound(formData: FormData) {
  await onPot(formData, (potId) => api().pots.settleRound({ potId }));
}
