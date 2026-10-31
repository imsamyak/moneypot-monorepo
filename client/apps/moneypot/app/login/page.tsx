import Link from 'next/link';
import { ActionForm, Field } from '@/components/ActionForm';
import { AuthShell } from '@/components/AuthShell';
import { login } from '@/lib/actions';

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  return (
    <AuthShell title="Welcome back" subtitle="Sign in to your pots and wallet.">
      <ActionForm action={login} submitLabel="Sign in">
        <input type="hidden" name="next" value={next ?? ''} />
        <Field label="Email" name="email" type="email" autoComplete="email" placeholder="you@example.com" required />
        <Field label="Password" name="password" type="password" autoComplete="current-password" required />
      </ActionForm>
      <p className="muted">
        New here? <Link href="/register" style={{ color: 'var(--brand)', fontWeight: 700 }}>Create an account</Link>
      </p>
    </AuthShell>
  );
}
