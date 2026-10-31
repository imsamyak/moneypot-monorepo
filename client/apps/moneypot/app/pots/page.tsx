import Link from 'next/link';
import { Icon } from '@/components/icons';
import { JoinWithCode } from '@/components/JoinWithCode';
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
            <h1 style={{ fontSize: '2rem', fontWeight: 850, lineHeight: 1.1 }}>
              {mine.items.length === 0 ? 'Start your first pot' : `${running} running, ${joining} still joining`}
            </h1>
            <p className="muted-on">Pots are by invitation. Create one and share its code, or enter a code you were given.</p>
          </div>
          <Link href="/pots/new" className="btn btn-gold btn-lg"><Icon name="plus" size={18} /> Create a pot</Link>
        </div>
      </section>

      <section className="card stack-sm">
        <div className="cluster" style={{ gap: '0.7rem' }}>
          <span className="icon-bubble icon-bubble-gold"><Icon name="gift" /></span>
          <div>
            <h2 className="section-title">Join with a code</h2>
            <p className="muted small">Someone invited you? Enter the code they sent.</p>
          </div>
        </div>
        <JoinWithCode />
      </section>

      {mine.items.length === 0 ? (
        <div className="card card-flat stack-sm">
          <h2 className="section-title">Nothing here yet</h2>
          <p className="muted">You are not in a pot. Create one, or join with a code above.</p>
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
