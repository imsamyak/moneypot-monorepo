import Link from 'next/link';
import { redirect } from 'next/navigation';
import { Avatar } from '@/components/Avatar';
import { Calculator } from '@/components/Calculator';
import { getUser } from '@/lib/session';

const STEPS = [
  { title: 'Invite your group', text: 'Create a pot, choose when bidding starts, and share the private invite code. There is no limit on members.' },
  { title: 'Pay to join', text: 'Your contribution goes in when you join. Until bidding starts you can still leave and get it back.' },
  { title: 'Bid for the pot', text: 'At the start time joining closes and bidding opens. Members who have not collected yet say the least they will accept. The lowest bid collects the pot.' },
  { title: 'Share the rest', text: 'What the collector leaves behind goes back to everyone else, straight to their wallets.' },
];

const EXAMPLE_BIDS = [
  { order: 4, amount: '₹41,000', width: 82, lead: true },
  { order: 7, amount: '₹44,500', width: 89, lead: false },
  { order: 2, amount: '₹47,000', width: 94, lead: false },
];

export default async function Home() {
  if (await getUser()) redirect('/pots');
  return (
    <div className="stack" style={{ gap: '3.5rem' }}>
      <section className="hero-landing">
        <div className="stack">
          <h1 className="hero-title">
            Bid for the pot. <span className="gradient-text">Lowest bid collects.</span>
          </h1>
          <p className="muted" style={{ fontSize: '1.1rem', maxWidth: '34rem' }}>
            Save with people you trust. Start a pot, invite friends with a private code, and everyone pays in. When bidding opens,
            whoever will settle for the least collects the pot, and the difference is shared back with the rest of the group.
          </p>
          <div className="cluster">
            <Link href="/register" className="btn btn-primary btn-lg">Create an account</Link>
            <Link href="/login" className="btn btn-lg">Sign in</Link>
          </div>
        </div>

        <div className="card stack" aria-hidden="true">
          <div className="between">
            <span className="eyebrow">Example, round 3 of 10</span>
            <span className="live">Live</span>
          </div>
          <div>
            <p className="muted small">The pot</p>
            <p className="big-number">₹50,000</p>
          </div>
          <div className="stack-sm">
            {EXAMPLE_BIDS.map((bid) => (
              <div key={bid.order} style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', alignItems: 'center', gap: '0.7rem' }}>
                <Avatar order={bid.order} size={28} />
                <div className="track"><div className={bid.lead ? 'track-fill' : 'track-fill track-fill-gray'} style={{ width: `${bid.width}%`, background: bid.lead ? undefined : 'var(--line)' }} /></div>
                <strong>{bid.amount}</strong>
              </div>
            ))}
          </div>
          <p className="muted small">Member 4 leads and would collect ₹41,000. The other ₹9,000 would be shared by the nine others.</p>
        </div>
      </section>

      <section className="stack">
        <h2 style={{ fontSize: '1.6rem', fontWeight: 700 }}>How it works</h2>
        <div className="steps-grid">
          {STEPS.map((step, index) => (
            <div className="card stack-sm" key={step.title}>
              <span className="num-badge">{index + 1}</span>
              <h3 style={{ fontWeight: 700 }}>{step.title}</h3>
              <p className="muted">{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="stack">
        <h2 style={{ fontSize: '1.6rem', fontWeight: 700 }}>Try the maths</h2>
        <Calculator />
      </section>

      <section className="card cta-band stack-sm">
        <h2 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Ready to start a pot?</h2>
        <p className="muted">Create one in a minute, or play a demo pot on your own first.</p>
        <div><Link href="/register" className="btn btn-primary btn-lg">Get started</Link></div>
      </section>
    </div>
  );
}
