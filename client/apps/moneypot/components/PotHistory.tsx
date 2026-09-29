import type { PotResponse } from '@moneypot/platform-sdk';
import { formatMoney, memberLabel } from '@/lib/format';

/** The rounds that are over: who won and what they were paid, and what was shared with everyone else. */
export function PotHistory({ pot, me }: { pot: PotResponse; me: string }) {
  if (pot.history.length === 0) return null;
  return (
    <div className="card">
      <h2 style={{ fontWeight: 800, marginBottom: '0.4rem' }}>Past rounds</h2>
      {pot.history.map((past) => {
        const payout = past.payout ?? pot.potAmount;
        const shared = pot.potAmount - payout;
        return (
          <div className="row" key={past.number}>
            <div>
              <div style={{ fontWeight: 600 }}>Round {past.number}: {memberLabel(past.winnerId, pot.members, me)}</div>
              <div className="muted" style={{ fontSize: '0.85rem' }}>
                {shared > 0
                  ? `${formatMoney(shared)} shared among the other ${pot.memberCount - 1} members`
                  : 'Won the whole pot, nothing left to share'}
              </div>
            </div>
            <strong>{formatMoney(payout)}</strong>
          </div>
        );
      })}
    </div>
  );
}
