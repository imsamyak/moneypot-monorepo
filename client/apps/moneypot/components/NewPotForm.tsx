'use client';

import { useActionState, useEffect, useState } from 'react';
import { createPot, type FormState } from '@/lib/actions';
import { formatDateTime, formatMoney } from '@/lib/format';
import { DurationField, Field } from './ActionForm';
import { Icon } from './icons';
import { SubmitButton } from './SubmitButton';

const initial: FormState = {};

/** A date and time as the value of a datetime-local input (the member's own time zone). */
function localInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** The create-pot form, with a summary that shows what the choices mean as they are made. */
export function NewPotForm() {
  const [state, action] = useActionState(createPot, initial);
  const [contribution, setContribution] = useState('5000');
  const [floor, setFloor] = useState(60);
  const [when, setWhen] = useState('');

  // the default start (tomorrow, same time) is worked out in the browser, which knows the time zone
  useEffect(() => setWhen(localInput(new Date(Date.now() + 24 * 3600 * 1000))), []);

  const startDate = when ? new Date(when) : null;
  const startsAt = startDate && !Number.isNaN(startDate.getTime()) ? startDate.toISOString() : '';
  const each = Math.round(Number(contribution.replace(/,/g, '')) * 100) || 0;
  const examplePot = 10 * each;
  const exampleMinBid = Math.ceil((examplePot * floor) / 100);

  const soon = (hours: number) => () => setWhen(localInput(new Date(Date.now() + hours * 3600 * 1000)));

  return (
    <form action={action} className="two-col">
      <div className="card stack">
        <Field label="Name" name="name" placeholder="Friends monthly pot" maxLength={60} required />

        <div>
          <label className="label" htmlFor="contribution">Each member pays every round</label>
          <div className="money-input">
            <span>₹</span>
            <input className="input" id="contribution" name="contribution" inputMode="decimal" value={contribution} onChange={(e) => setContribution(e.target.value)} required />
          </div>
          <div className="cluster" style={{ marginTop: '0.5rem' }}>
            {['500', '1000', '5000', '10000'].map((amount) => (
              <button type="button" key={amount} className="chip-btn" onClick={() => setContribution(amount)}>₹{Number(amount).toLocaleString('en-IN')}</button>
            ))}
          </div>
        </div>

        <div>
          <label className="label" htmlFor="when">Bidding starts</label>
          <input className="input" id="when" type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} required />
          <input type="hidden" name="startsAt" value={startsAt} />
          <div className="cluster" style={{ marginTop: '0.5rem' }}>
            <button type="button" className="chip-btn" onClick={soon(1)}>In 1 hour</button>
            <button type="button" className="chip-btn" onClick={soon(24)}>Tomorrow</button>
            <button type="button" className="chip-btn" onClick={soon(72)}>In 3 days</button>
            <button type="button" className="chip-btn" onClick={soon(24 * 7)}>In a week</button>
          </div>
          <p className="muted small" style={{ marginTop: '0.4rem' }}>
            People can join until then, and not after. Whoever has paid by then is the group.
          </p>
        </div>

        <DurationField label="Bidding stays open for" name="bid" hours={24} hint="Each round, from when it opens: up to 7 days (168 hours). It also ends when you end it." />

        <div className="stack-sm">
          <label className="label" htmlFor="minBidPercent">Lowest bid allowed: <strong>{floor}%</strong> of the pot</label>
          <input id="minBidPercent" name="minBidPercent" type="range" min={10} max={100} step={5} value={floor} onChange={(e) => setFloor(Number(e.target.value))} />
          <p className="muted small">A lower floor means bigger discounts for the winner, and bigger dividends for everyone else.</p>
        </div>

        {state.error && <p className="notice notice-error" role="alert">{state.error}</p>}
        <div><SubmitButton>Create pot and pay my share</SubmitButton></div>
      </div>

      <aside className="hero-card stack" style={{ position: 'sticky', top: '5rem' }}>
        <span className="chip chip-glass" style={{ justifySelf: 'start' }}><Icon name="coin" size={14} /> How it will work</span>
        <ul className="stack-sm" style={{ listStyle: 'none', padding: 0 }}>
          <li className="cluster" style={{ flexWrap: 'nowrap', alignItems: 'flex-start' }}>
            <Icon name="users" size={18} />
            <span>You get a private invite link. There is no limit on members.</span>
          </li>
          <li className="cluster" style={{ flexWrap: 'nowrap', alignItems: 'flex-start' }}>
            <Icon name="wallet" size={18} />
            <span>Everyone pays <strong>{formatMoney(each)}</strong> when they join, you too. They can leave for a full refund until bidding starts.</span>
          </li>
          <li className="cluster" style={{ flexWrap: 'nowrap', alignItems: 'flex-start' }}>
            <Icon name="clock" size={18} />
            <span>
              {startDate && startsAt ? <>Joining closes and bidding opens <strong>{formatDateTime(startsAt)}</strong>.</> : 'Choose when bidding starts.'}
            </span>
          </li>
        </ul>
        <div className="stat" style={{ marginTop: 0 }}>
          <div className="stat-label">Example with 10 members</div>
          <div className="stat-value">{formatMoney(examplePot)} pot</div>
          <p className="small" style={{ opacity: 0.85 }}>
            Lowest bid allowed {formatMoney(exampleMinBid)}. If it wins, the other 9 share {formatMoney(examplePot - exampleMinBid)}:
            {' '}{formatMoney(Math.floor((examplePot - exampleMinBid) / 9))} each.
          </p>
        </div>
      </aside>
    </form>
  );
}
