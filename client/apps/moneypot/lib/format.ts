/** Money is a whole number of paise in the services; the screen shows rupees. */
const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

export function formatMoney(paise: number): string {
  return rupees.format(paise / 100);
}

/** "5000" or "5,000.50" typed by a member, as paise; NaN when it is not a number. */
export function rupeesToPaise(text: FormDataEntryValue | null): number {
  const value = Number(String(text ?? '').replace(/,/g, '').trim());
  return Number.isFinite(value) ? Math.round(value * 100) : Number.NaN;
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

/** Members have no names in MoneyPot yet, so they are shown by the order they joined. */
export function memberLabel(userId: string, members: { userId: string; joinOrder: number }[], me: string): string {
  const member = members.find((m) => m.userId === userId);
  const label = member ? `Member ${member.joinOrder}` : 'Someone';
  return userId === me ? `${label} (you)` : label;
}
