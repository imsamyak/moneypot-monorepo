'use client';

import { useState } from 'react';
import { formatMoney } from '@/lib/format';

/** "Try the maths": move the sliders and see who gets what in a round. The same rules the services use. */
export function Calculator() {
  const [members, setMembers] = useState(10);
  const [each, setEach] = useState(5000);
  const [percent, setPercent] = useState(82);

  const pot = members * each * 100;
  const bid = Math.round((pot * percent) / 100);
  const left = pot - bid;
  const share = Math.floor(left / (members - 1));

  return (
    <div className="two-col">
      <div className="stack">
        <div>
          <label className="label" htmlFor="calc-members">Members: <strong>{members}</strong></label>
          <input id="calc-members" type="range" min={3} max={30} value={members} onChange={(e) => setMembers(Number(e.target.value))} />
        </div>
        <div>
          <p className="label">Each pays every round</p>
          <div className="cluster">
            {[500, 1000, 5000, 10000].map((amount) => (
              <button type="button" key={amount} className={each === amount ? 'chip-btn chip-btn-on' : 'chip-btn'} onClick={() => setEach(amount)}>
                ₹{amount.toLocaleString('en-IN')}
              </button>
            ))}
          </div>
        </div>
        <div>
          <label className="label" htmlFor="calc-bid">The winning bid: <strong>{percent}%</strong> of the pot</label>
          <input id="calc-bid" type="range" min={50} max={100} value={percent} onChange={(e) => setPercent(Number(e.target.value))} />
          <p className="muted small">The lower someone is willing to go, the more is left for the group.</p>
        </div>
      </div>

      <div className="hero-card stack">
        <div>
          <p className="muted-on">The pot this round</p>
          <p className="big-number">{formatMoney(pot)}</p>
        </div>
        <div className="split-bar" aria-hidden="true">
          <div style={{ width: `${percent}%`, background: '#fbbf24' }} />
          <div style={{ width: `${100 - percent}%`, background: '#5eead4' }} />
        </div>
        <div className="between">
          <div>
            <p className="eyebrow" style={{ color: '#fde68a' }}>Winner gets</p>
            <p style={{ fontSize: '1.4rem', fontWeight: 800 }}>{formatMoney(bid)}</p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p className="eyebrow" style={{ color: '#99f6e4' }}>Each of the other {members - 1}</p>
            <p style={{ fontSize: '1.4rem', fontWeight: 800 }}>+{formatMoney(share)}</p>
          </div>
        </div>
        <p className="muted-on small">After {members} rounds everyone has won once, and everyone has also been paid a share nine times out of ten.</p>
      </div>
    </div>
  );
}
