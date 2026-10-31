import Link from 'next/link';
import { Icon } from '@/components/icons';
import { PotCard } from '@/components/PotCard';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

export default async function PotsPage() {
  await requireUser('/pots');
  const mine = await api().pots.listPots();
  const running = mine.items.filter((pot) => pot.status === 'ACTIVE').length;
  const joining = mine.items.filter((pot) => pot.status === 'OPEN').length;

  return (
    <div className="stack" style={{ gap: '2rem' }}>
      <section className="hero-card">
        <div className="between">
          <div className="stack-sm">
            <span className="chip chip-glass" style={{ justifySelf: 'start' }}><Icon name="users" size={14} /> Your pots</span>
            <h1 style={{ fontSize: '1.9rem', fontWeight: 850, lineHeight: 1.1 }}>
              {mine.items.length === 0 ? 'Start your first pot' : `${running} running, ${joining} still joining`}
            </h1>
            <p className="muted-on">Pots are by invitation. Create one and share its link, or open a link someone sent you.</p>
          </div>
          <Link href="/pots/new" className="btn btn-white btn-lg"><Icon name="plus" size={18} /> Create a pot</Link>
        </div>
      </section>

      {mine.items.length === 0 ? (
        <div className="card card-flat stack-sm">
          <h2 className="section-title">Nothing here yet</h2>
          <p className="muted">You are not in a pot. Create one, or ask a friend for their invite link and open it to join.</p>
        </div>
      ) : (
        <section className="stack">
          <h2 className="section-title">Your pots</h2>
          <div className="grid-pots">{mine.items.map((pot, i) => <PotCard key={pot.id} pot={pot} index={i} />)}</div>
        </section>
      )}
    </div>
  );
}
