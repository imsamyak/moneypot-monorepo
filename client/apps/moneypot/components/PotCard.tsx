import Link from 'next/link';
import type { PotSummaryResponse } from '@moneypot/platform-sdk';
import { formatDateTime, formatMoney } from '@/lib/format';
import { Icon } from './icons';

const STATUS = {
  OPEN: { label: 'Joining open', chip: 'chip chip-warn' },
  ACTIVE: { label: 'Running', chip: 'chip' },
  COMPLETED: { label: 'Completed', chip: 'chip chip-muted' },
  CANCELLED: { label: 'Cancelled', chip: 'chip chip-bad' },
} as const;

/** A pot in a list: its size, and where it is (joining until the start time, then which round). */
export function PotCard({ pot, index = 0 }: { pot: PotSummaryResponse; index?: number }) {
  const status = STATUS[pot.status];
  const progress = pot.status === 'ACTIVE' ? Math.round(((pot.currentRound - 1) / pot.memberCount) * 100) : pot.status === 'COMPLETED' ? 100 : 0;
  return (
    <Link href={`/pots/${pot.id}`} className="card card-link stack-sm" style={{ animationDelay: `${index * 60}ms` }}>
      <div className="between" style={{ flexWrap: 'nowrap' }}>
        <span className="icon-bubble"><Icon name="pot" /></span>
        <span className={status.chip}>{status.label}</span>
      </div>
      <h3 style={{ fontWeight: 800, fontSize: '1.1rem' }}>{pot.name}</h3>
      <p className="big-number" style={{ fontSize: '1.9rem' }}>{formatMoney(pot.potAmount)}</p>
      {(pot.status === 'ACTIVE' || pot.status === 'COMPLETED') && (
        <div className="track"><div className="track-fill" style={{ width: `${progress}%` }} /></div>
      )}
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
