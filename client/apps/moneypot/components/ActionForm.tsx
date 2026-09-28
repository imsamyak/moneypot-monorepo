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
    <form action={formAction} style={{ display: 'grid', gap: '0.9rem' }}>
      {children}
      {state.error && <p className="notice notice-error" role="alert">{state.error}</p>}
      {state.message && <p className="notice" role="status">{state.message}</p>}
      <div>
        <SubmitButton>{submitLabel}</SubmitButton>
      </div>
    </form>
  );
}

/** A labelled input for an ActionForm. */
export function Field({ label, hint, ...input }: { label: string; hint?: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div>
      <label className="label" htmlFor={input.name}>{label}</label>
      <input className="input" id={input.name} {...input} />
      {hint && <p className="muted" style={{ fontSize: '0.8rem', marginTop: '0.25rem' }}>{hint}</p>}
    </div>
  );
}
