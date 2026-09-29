import Link from 'next/link';
import { PotCard } from '@/components/PotCard';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

export default async function PotsPage() {
  await requireUser('/pots');
  const [mine, open] = await Promise.all([api().pots.listPots({ scope: 'mine' }), api().pots.listPots({ scope: 'open' })]);
  const mineIds = new Set(mine.items.map((pot) => pot.id));
  const joinable = open.items.filter((pot) => !mineIds.has(pot.id));

  return (
    <div style={{ display: 'grid', gap: '2rem' }}>
      <section>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.8rem' }}>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Your pots</h1>
          <Link href="/pots/new" className="btn btn-primary">Create a pot</Link>
        </div>
        {mine.items.length === 0 && <p className="muted">You are not in a pot yet. Create one, or join one below.</p>}
        <div className="grid-pots">{mine.items.map((pot) => <PotCard key={pot.id} pot={pot} />)}</div>
      </section>

      <section>
        <h2 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '0.8rem' }}>Open to join</h2>
        {joinable.length === 0 && <p className="muted">No open pots right now.</p>}
        <div className="grid-pots">{joinable.map((pot) => <PotCard key={pot.id} pot={pot} />)}</div>
      </section>
    </div>
  );
}
