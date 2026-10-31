/** Money is a whole number of paise in the services; the screen shows rupees. */
const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
const rupeesWhole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export function formatMoney(paise: number): string {
  return paise % 100 === 0 ? rupeesWhole.format(paise / 100) : rupees.format(paise / 100);
}

/** "5000" or "5,000.50" typed by a member, as paise; NaN when it is not a number. */
export function rupeesToPaise(text: FormDataEntryValue | null): number {
  const value = Number(String(text ?? '').replace(/,/g, '').trim());
  return Number.isFinite(value) ? Math.round(value * 100) : Number.NaN;
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

/** 5400 seconds as "1 hour 30 minutes"; only the parts that are not zero. */
export function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const plural = (n: number, unit: string) => `${n} ${unit}${n === 1 ? '' : 's'}`;
  const parts = [hours && plural(hours, 'hour'), minutes && plural(minutes, 'minute')].filter(Boolean);
  return parts.length ? parts.join(' ') : `${seconds} seconds`;
}

/** A colour for a member, from the order they joined, so a member looks the same on every screen. */
export function memberHue(order: number): number {
  return (order * 137) % 360;
}

/** Members have no names in MoneyPot yet, so they are shown by the order they joined. */
export function memberLabel(userId: string, members: { userId: string; joinOrder: number }[], me: string): string {
  const member = members.find((m) => m.userId === userId);
  const label = member ? `Member ${member.joinOrder}` : 'Someone';
  return userId === me ? `${label} (you)` : label;
}
