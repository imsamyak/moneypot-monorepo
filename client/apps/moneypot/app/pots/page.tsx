import Link from 'next/link';
import { JoinWithCode } from '@/components/JoinWithCode';
import { PotCard } from '@/components/PotCard';
import { SubmitButton } from '@/components/SubmitButton';
import { createDemoPot } from '@/lib/actions';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

export default async function PotsPage() {
  await requireUser('/pots');
  const mine = await api().pots.listPots();

  return (
    <div className="stack" style={{ gap: '1.75rem' }}>
      <div className="between">
        <div>
          <h1 style={{ fontSize: '1.6rem', fontWeight: 700 }}>Your pots</h1>
          <p className="muted">Pots are by invitation. Create one and share its code, or enter a code you were given.</p>
        </div>
        <Link href="/pots/new" className="btn btn-primary">Create a pot</Link>
      </div>

      <div className="two-col">
        <section className="card stack-sm">
          <h2 className="section-title">Join with a code</h2>
          <p className="muted small">Someone invited you? Enter the code they sent.</p>
          <JoinWithCode />
        </section>
        <section className="card stack-sm">
          <h2 className="section-title">Try a demo pot</h2>
          <p className="muted small">Play a whole pot on your own: four simulated members, play money, your wallet is not used.</p>
          <form action={createDemoPot}><SubmitButton primary={false}>Start a demo pot</SubmitButton></form>
        </section>
      </div>

      {mine.items.length === 0 ? (
        <div className="card card-flat">
          <p className="muted">You are not in a pot yet. Create one, join with a code, or start a demo pot above.</p>
        </div>
      ) : (
        <section className="stack">
          <h2 className="section-title">Your pots</h2>
          <div className="grid-pots">{mine.items.map((pot) => <PotCard key={pot.id} pot={pot} />)}</div>
        </section>
      )}
    </div>
  );
}
