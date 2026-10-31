import Link from 'next/link';
import { ActionForm, Field } from '@/components/ActionForm';
import { AuthShell } from '@/components/AuthShell';
import { register } from '@/lib/actions';

export default async function RegisterPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  return (
    <AuthShell title="Create your account" subtitle="It takes a few seconds. You can start or join a pot right away.">
      <ActionForm action={register} submitLabel="Create account">
        <input type="hidden" name="next" value={next ?? ''} />
        <Field label="Email" name="email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Field label="Password" name="password" type="password" autoComplete="new-password" minLength={8} hint="At least 8 characters." required />
      </ActionForm>
      <p className="muted">
        Already have an account? <Link href="/login" style={{ color: 'var(--brand)', fontWeight: 700 }}>Sign in</Link>
      </p>
    </AuthShell>
  );
}
