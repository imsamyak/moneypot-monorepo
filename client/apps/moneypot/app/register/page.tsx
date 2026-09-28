import Link from 'next/link';
import { ActionForm, Field } from '@/components/ActionForm';
import { register } from '@/lib/actions';

export default async function RegisterPage({ searchParams }: { searchParams: Promise<{ next?: string }> }) {
  const { next } = await searchParams;
  return (
    <div className="card" style={{ maxWidth: '26rem', margin: '2rem auto' }}>
      <h1 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '1rem' }}>Create your account</h1>
      <ActionForm action={register} submitLabel="Create account">
        <input type="hidden" name="next" value={next ?? ''} />
        <Field label="Email" name="email" type="email" autoComplete="email" required />
        <Field
          label="Password"
          name="password"
          type="password"
          autoComplete="new-password"
          minLength={8}
          hint="At least 8 characters."
          required
        />
      </ActionForm>
      <p className="muted" style={{ marginTop: '1rem' }}>
        Already have an account? <Link href="/login" style={{ color: 'var(--brand)', fontWeight: 600 }}>Sign in</Link>
      </p>
    </div>
  );
}
