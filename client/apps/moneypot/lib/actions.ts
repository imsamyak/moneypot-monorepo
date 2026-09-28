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

export async function createPot(_: FormState, formData: FormData): Promise<FormState> {
  await requireUser('/pots/new');
  let potId: string;
  try {
    const pot = await api().pots.createPot({
      name: String(formData.get('name') ?? ''),
      memberCount: Number(formData.get('memberCount')),
      contribution: rupeesToPaise(formData.get('contribution')),
      minBidPercent: Number(formData.get('minBidPercent')),
      bidWindowSeconds: Math.round(Number(formData.get('bidWindowMinutes')) * 60),
    });
    potId = pot.id;
  } catch (error) {
    return { error: errorMessage(error) };
  }
  redirect(`/pots/${potId}`);
}

/** Runs one action on a pot, then shows the pot again (with the error, if there was one). */
async function onPot(formData: FormData, run: (potId: string) => Promise<unknown>) {
  const potId = String(formData.get('potId'));
  await requireUser(`/pots/${potId}`);
  try {
    await run(potId);
  } catch (error) {
    redirect(`/pots/${potId}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath(`/pots/${potId}`);
  redirect(`/pots/${potId}`);
}

export async function joinPot(formData: FormData) {
  await onPot(formData, (potId) => api().pots.joinPot({ potId }));
}

export async function startPot(formData: FormData) {
  await onPot(formData, (potId) => api().pots.startPot({ potId }));
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
