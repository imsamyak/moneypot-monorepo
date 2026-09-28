import Link from 'next/link';
import { ActionForm, Field } from '@/components/ActionForm';
import { login } from '@/lib/actions';

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  return (
    <div className="card" style={{ maxWidth: '26rem', margin: '2rem auto' }}>
      <h1 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '1rem' }}>Sign in</h1>
      <ActionForm action={login} submitLabel="Sign in">
        <input type="hidden" name="next" value={next ?? ''} />
        <Field label="Email" name="email" type="email" autoComplete="email" required />
        <Field label="Password" name="password" type="password" autoComplete="current-password" required />
      </ActionForm>
      <p className="muted" style={{ marginTop: '1rem' }}>
        New here? <Link href="/register" style={{ color: 'var(--brand)', fontWeight: 600 }}>Create an account</Link>
      </p>
    </div>
  );
}
