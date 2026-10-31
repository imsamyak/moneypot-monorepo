import type { PotResponse } from '@moneypot/platform-sdk';
import { formatMoney } from '@/lib/format';
import { MemberTag } from './Avatar';
import { Icon } from './icons';

/** The rounds that are over: who collected and what they were paid, and what was shared with everyone else. */
export function PotHistory({ pot, me }: { pot: PotResponse; me: string }) {
  if (pot.history.length === 0) return null;
  return (
    <div className="card stack-sm">
      <h2 className="section-title">Past rounds</h2>
      <div>
        {pot.history.map((past) => {
          const payout = past.payout ?? pot.potAmount;
          const shared = pot.potAmount - payout;
          return (
            <div className="row" key={past.number}>
              <div className="cluster" style={{ flexWrap: 'nowrap', gap: '0.8rem' }}>
                <span className="icon-bubble icon-bubble-gold"><Icon name="trophy" /></span>
                <div>
                  <div style={{ fontWeight: 700 }}>Round {past.number}</div>
                  <MemberTag userId={past.winnerId} members={pot.members} me={me} size={22} />
                  <div className="muted small">
                    {shared > 0 ? `${formatMoney(shared)} shared among the other ${pot.memberCount - 1} members` : 'Collected the whole pot, nothing left to share'}
                  </div>
                </div>
              </div>
              <strong>{formatMoney(payout)}</strong>
            </div>
          );
        })}
      </div>
    </div>
  );
}
