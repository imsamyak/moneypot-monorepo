import Link from 'next/link';
import type { PotSummaryResponse } from '@moneypot/platform-sdk';
import { formatMoney } from '@/lib/format';

const STATUS_LABEL = { OPEN: 'Open to join', ACTIVE: 'Running', COMPLETED: 'Completed' } as const;

export function PotCard({ pot }: { pot: PotSummaryResponse }) {
  return (
    <Link href={`/pots/${pot.id}`} className="card" style={{ display: 'block' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.5rem' }}>
        <strong>{pot.name}</strong>
        <span className={pot.status === 'OPEN' ? 'badge badge-warn' : 'badge'}>{STATUS_LABEL[pot.status]}</span>
      </div>
      <p style={{ fontSize: '1.5rem', fontWeight: 800, margin: '0.5rem 0 0.2rem' }}>{formatMoney(pot.potAmount)}</p>
      <p className="muted" style={{ fontSize: '0.9rem' }}>
        {formatMoney(pot.contribution)} each, {pot.joined} of {pot.memberCount} members
        {pot.status === 'ACTIVE' && ` · round ${pot.currentRound} of ${pot.memberCount}`}
      </p>
    </Link>
  );
}
