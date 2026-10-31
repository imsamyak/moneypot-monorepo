'use client';

import { useActionState, useState } from 'react';
import { deposit, type FormState } from '@/lib/actions';
import { SubmitButton } from './SubmitButton';

const initial: FormState = {};

/** The top-up form, with quick amounts. */
export function DepositForm() {
  const [state, action] = useActionState(deposit, initial);
  const [amount, setAmount] = useState('');
  return (
    <form action={action} className="stack">
      <div>
        <label className="label" htmlFor="amount">Amount</label>
        <div className="money-input">
          <span>₹</span>
          <input className="input" id="amount" name="amount" inputMode="decimal" placeholder="5000" value={amount} onChange={(e) => setAmount(e.target.value)} required />
        </div>
        <div className="cluster" style={{ marginTop: '0.5rem' }}>
          {['500', '1000', '5000', '10000'].map((quick) => (
            <button type="button" key={quick} className="chip-btn" onClick={() => setAmount(quick)}>₹{Number(quick).toLocaleString('en-IN')}</button>
          ))}
        </div>
        <p className="muted small" style={{ marginTop: '0.4rem' }}>Up to ₹1,00,000 at a time. This is a test top-up, there is no bank behind it.</p>
      </div>
      {state.error && <p className="notice notice-error" role="alert">{state.error}</p>}
      {state.message && <p className="notice" role="status">{state.message}</p>}
      <div><SubmitButton>Add to wallet</SubmitButton></div>
    </form>
  );
}
