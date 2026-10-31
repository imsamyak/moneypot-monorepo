'use client';

import { useActionState } from 'react';
import type { FormState } from '@/lib/actions';
import { SubmitButton } from './SubmitButton';

/** A form that calls a server action and shows the error or the message it answers with. */
export function ActionForm({
  action,
  submitLabel,
  children,
}: {
  action: (state: FormState, formData: FormData) => Promise<FormState>;
  submitLabel: string;
  children: React.ReactNode;
}) {
  const [state, formAction] = useActionState(action, {});
  return (
    <form action={formAction} className="stack">
      {children}
      {state.error && <p className="notice notice-error" role="alert">{state.error}</p>}
      {state.message && <p className="notice" role="status">{state.message}</p>}
      <div>
        <SubmitButton>{submitLabel}</SubmitButton>
      </div>
    </form>
  );
}

/** A labelled input for a form. */
export function Field({ label, hint, ...input }: { label: string; hint?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div>
      <label className="label" htmlFor={input.name}>{label}</label>
      <input className="input" id={input.name} {...input} />
      {hint && <p className="muted small" style={{ marginTop: '0.3rem' }}>{hint}</p>}
    </div>
  );
}

/** How long something lasts, typed as hours and minutes (inputs named {name}Hours and {name}Minutes). */
export function DurationField({
  label,
  name,
  hint,
  hours = 0,
  minutes = 0,
}: {
  label: string;
  name: string;
  hint?: string;
  hours?: number;
  minutes?: number;
}) {
  return (
    <fieldset style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="label">{label}</legend>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.7rem' }}>
        <label style={{ display: 'grid', gap: '0.25rem' }}>
          <input className="input" type="number" name={`${name}Hours`} min={0} max={168} defaultValue={hours} />
          <span className="muted small">Hours</span>
        </label>
        <label style={{ display: 'grid', gap: '0.25rem' }}>
          <input className="input" type="number" name={`${name}Minutes`} min={0} max={59} defaultValue={minutes} />
          <span className="muted small">Minutes</span>
        </label>
      </div>
      {hint && <p className="muted small" style={{ marginTop: '0.3rem' }}>{hint}</p>}
    </fieldset>
  );
}
