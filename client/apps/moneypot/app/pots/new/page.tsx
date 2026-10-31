import Link from 'next/link';
import { NewPotForm } from '@/components/NewPotForm';
import { requireUser } from '@/lib/session';

export default async function NewPotPage() {
  await requireUser('/pots/new');
  return (
    <div className="stack">
      <div className="stack-sm">
        <Link href="/pots" className="muted small">← Back to pots</Link>
        <h1 style={{ fontSize: '1.9rem', fontWeight: 850 }}>Create a pot</h1>
        <p className="muted">Choose how much each member pays and when bidding starts, then invite your group.</p>
      </div>
      <NewPotForm />
    </div>
  );
}
