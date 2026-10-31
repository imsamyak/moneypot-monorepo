import Link from 'next/link';
import type { PotSummaryResponse } from '@moneypot/platform-sdk';
import { formatDateTime, formatMoney } from '@/lib/format';
import { PotArt } from './PotArt';

const STATUS = {
  OPEN: { label: 'Joining open', chip: 'chip chip-warn' },
  ACTIVE: { label: 'Running', chip: 'chip' },
  COMPLETED: { label: 'Completed', chip: 'chip chip-muted' },
  CANCELLED: { label: 'Cancelled', chip: 'chip chip-bad' },
} as const;

/** A pot in a list: its size, a pot that is as full as the pot is, and where it is (joining until the start time, then which round). */
export function PotCard({ pot, index = 0 }: { pot: PotSummaryResponse; index?: number }) {
  const status = STATUS[pot.status];
  const progress = pot.status === 'ACTIVE' ? (pot.currentRound - 1) / pot.memberCount : pot.status === 'COMPLETED' ? 1 : 0;
  const level = pot.status === 'OPEN' ? Math.min(1, 0.15 + (pot.memberCount / 10) * 0.85) : pot.status === 'ACTIVE' ? 1 - progress * 0.7 : 0.1;
  return (
    <Link href={`/pots/${pot.id}`} className="card card-link pot-card" style={{ animationDelay: `${index * 70}ms` }}>
      <div className="stack-sm">
        <span className={status.chip} style={{ justifySelf: 'start' }}>{status.label}</span>
        <h3 style={{ fontWeight: 800, fontSize: '1.15rem' }}>{pot.name}</h3>
        <p className="big-number" style={{ fontSize: '1.8rem' }}>{formatMoney(pot.potAmount)}</p>
        <p className="muted small">
          {pot.status === 'OPEN' && `${pot.memberCount} joined · bidding starts ${formatDateTime(pot.startsAt)}`}
          {pot.status === 'ACTIVE' && `Round ${pot.currentRound} of ${pot.memberCount}`}
          {pot.status === 'COMPLETED' && `All ${pot.memberCount} rounds done`}
          {pot.status === 'CANCELLED' && 'Not enough members joined'}
          {' · '}{formatMoney(pot.contribution)} each
        </p>
        {pot.status === 'ACTIVE' && <div className="track"><div className="track-fill" style={{ width: `${Math.round(progress * 100)}%` }} /></div>}
      </div>
      <PotArt level={level} size={96} />
    </Link>
  );
}
