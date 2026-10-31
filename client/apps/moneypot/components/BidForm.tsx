'use client';

import { useState } from 'react';
import { placeBid } from '@/lib/actions';
import { formatMoney, rupeesToPaise } from '@/lib/format';
import { SubmitButton } from './SubmitButton';

/** The bid form, with a preview of what the member would get and what the others would share. */
export function BidForm({
  potId,
  potAmount,
  minBid,
  memberCount,
  lowest,
  mine,
}: {
  potId: string;
  potAmount: number;
  minBid: number;
  memberCount: number;
  /** The lowest bid so far, in paise. */
  lowest: number | null;
  /** The member's own bid, in paise. */
  mine: number | null;
}) {
  const [text, setText] = useState('');
  const bid = rupeesToPaise(text);
  const valid = Number.isFinite(bid) && bid >= minBid && bid <= potAmount && (mine === null || bid < mine);
  const share = memberCount > 1 && valid ? Math.floor((potAmount - bid) / (memberCount - 1)) : 0;
  const reference = mine ?? lowest ?? potAmount;

  // quick steps below the current best, never under the lowest allowed bid
  const steps = [50, 100, 500].map((rupees) => Math.max(minBid, reference - rupees * 100)).filter((value, i, all) => value < reference && all.indexOf(value) === i);

  return (
    <form action={placeBid} className="stack-sm">
      <input type="hidden" name="potId" value={potId} />
      <label className="label" htmlFor="amount">{mine ? 'Lower your bid' : 'Your bid'}</label>
      <div className="money-input">
        <span>₹</span>
        <input className="input" id="amount" name="amount" inputMode="decimal" placeholder={String(Math.round(reference / 100) - 1)} value={text} onChange={(e) => setText(e.target.value)} required />
      </div>
      {steps.length > 0 && (
        <div className="cluster">
          {steps.map((value) => (
            <button type="button" key={value} className="chip-btn" onClick={() => setText(String(value / 100))}>{formatMoney(value)}</button>
          ))}
        </div>
      )}
      <p className="muted small">
        {valid ? (
          <>
            If you win at <strong>{formatMoney(bid)}</strong> you get that, and the other {memberCount - 1} members share{' '}
            {formatMoney(potAmount - bid)} ({formatMoney(share)} each).
          </>
        ) : (
          <>
            Between {formatMoney(minBid)} and {formatMoney(potAmount)}. The lowest bid wins
            {mine ? `, and you can only go lower than your bid of ${formatMoney(mine)}` : ''}.
          </>
        )}
      </p>
      <div><SubmitButton>{mine ? 'Lower my bid' : 'Place bid'}</SubmitButton></div>
    </form>
  );
}
