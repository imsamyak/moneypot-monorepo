import Link from 'next/link';
import type { PotSummaryResponse } from '@moneypot/platform-sdk';
import { formatDateTime, formatMoney } from '@/lib/format';

const STATUS = {
  OPEN: { label: 'Joining open', chip: 'chip chip-warn' },
  ACTIVE: { label: 'Running', chip: 'chip' },
  COMPLETED: { label: 'Completed', chip: 'chip chip-muted' },
  CANCELLED: { label: 'Cancelled', chip: 'chip chip-bad' },
} as const;

/** A pot in a list: its size and where it is (joining until the start time, then which round). */
export function PotCard({ pot }: { pot: PotSummaryResponse }) {
  const status = STATUS[pot.status];
  const progress = pot.status === 'ACTIVE' ? (pot.currentRound - 1) / pot.memberCount : pot.status === 'COMPLETED' ? 1 : 0;
  return (
    <Link href={`/pots/${pot.id}`} className="card card-link pot-card stack-sm">
      <div className="between" style={{ flexWrap: 'nowrap' }}>
        <h3 style={{ fontWeight: 700 }}>{pot.name}</h3>
        <span className={status.chip}>{status.label}</span>
      </div>
      <p className="big-number" style={{ fontSize: '1.7rem' }}>{formatMoney(pot.potAmount)}</p>
      {pot.status === 'ACTIVE' && <div className="track"><div className="track-fill" style={{ width: `${Math.round(progress * 100)}%` }} /></div>}
      <p className="muted small">
        {pot.status === 'OPEN' && `${pot.memberCount} joined · bidding starts ${formatDateTime(pot.startsAt)}`}
        {pot.status === 'ACTIVE' && `Round ${pot.currentRound} of ${pot.memberCount}`}
        {pot.status === 'COMPLETED' && `All ${pot.memberCount} rounds done`}
        {pot.status === 'CANCELLED' && 'Not enough members joined'}
        {' · '}{formatMoney(pot.contribution)} each
      </p>
    </Link>
  );
}
