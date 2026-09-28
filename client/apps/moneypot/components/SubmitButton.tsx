'use client';

import { useFormStatus } from 'react-dom';

/** A submit button that is disabled while its form is being sent. */
export function SubmitButton({ children, primary = true }: { children: React.ReactNode; primary?: boolean }) {
  const { pending } = useFormStatus();
  return (
    <button className={primary ? 'btn btn-primary' : 'btn'} type="submit" disabled={pending}>
      {pending ? 'Please wait...' : children}
    </button>
  );
}
