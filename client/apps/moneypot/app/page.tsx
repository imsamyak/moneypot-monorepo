import Link from 'next/link';
import { redirect } from 'next/navigation';
import { Avatar } from '@/components/Avatar';
import { Icon, type IconName } from '@/components/icons';
import { getUser } from '@/lib/session';

const STEPS: { icon: IconName; title: string; text: string }[] = [
  { icon: 'users', title: 'Invite your group', text: 'Create a pot, choose when bidding starts, and share the private invite link. There is no limit on members.' },
  { icon: 'wallet', title: 'Pay to join', text: 'Your contribution goes in when you join. Until bidding starts you can still leave and get it back.' },
  { icon: 'gavel', title: 'Bid for the pot', text: 'At the start time joining closes and bidding opens. Members who have not won yet say what they will accept. The lowest bid wins.' },
  { icon: 'gift', title: 'Share the rest', text: 'What the winner leaves goes back to everyone else, straight to their wallets.' },
];

export default async function Home() {
  if (await getUser()) redirect('/pots');
  return (
    <div className="stack" style={{ gap: '3rem' }}>
      <section className="hero-landing">
        <div className="stack">
          <span className="chip" style={{ justifySelf: 'start' }}><Icon name="bolt" size={14} /> Save together</span>
          <h1 className="hero-title">
            Bid for the pot. <span className="gradient-text">Lowest bid wins.</span>
          </h1>
          <p className="muted" style={{ fontSize: '1.15rem', maxWidth: '34rem' }}>
            A modern kameti, by invitation only. Friends join with a private link and pay in, bidding opens at a time you set,
            and whatever the winner leaves behind is shared with everyone else.
          </p>
          <div className="cluster">
            <Link href="/register" className="btn btn-primary btn-lg">Create an account</Link>
            <Link href="/login" className="btn btn-lg">Sign in</Link>
          </div>
        </div>

        <div className="hero-card float" aria-hidden="true">
          <div className="between">
            <span className="chip chip-glass">Round 3 of 10</span>
            <span className="live">Live</span>
          </div>
          <p className="muted-on" style={{ marginTop: '1rem' }}>The pot</p>
          <p className="big-number">₹50,000</p>
          <div className="stack-sm" style={{ marginTop: '1.2rem' }}>
            {[
              { order: 4, amount: '₹41,000', width: 82, lead: true },
              { order: 7, amount: '₹44,500', width: 89, lead: false },
              { order: 2, amount: '₹47,000', width: 94, lead: false },
            ].map((bid) => (
              <div key={bid.order} style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', alignItems: 'center', gap: '0.7rem' }}>
                <Avatar order={bid.order} size={30} />
                <div className="track" style={{ background: 'rgba(255,255,255,0.2)' }}>
                  <div className="track-fill" style={{ width: `${bid.width}%`, background: bid.lead ? '#fbbf24' : 'rgba(255,255,255,0.75)' }} />
                </div>
                <strong>{bid.amount}</strong>
              </div>
            ))}
          </div>
          <p className="muted-on small" style={{ marginTop: '1rem' }}>
            Member 4 leads and would win ₹41,000. The other ₹9,000 would be shared by the nine others.
          </p>
        </div>
      </section>

      <section className="stack">
        <h2 className="section-title" style={{ fontSize: '1.6rem' }}>How it works</h2>
        <div className="steps-grid">
          {STEPS.map((step, index) => (
            <div className="card stack-sm" key={step.title} style={{ animationDelay: `${index * 70}ms` }}>
              <span className="icon-bubble"><Icon name={step.icon} /></span>
              <h3 style={{ fontWeight: 800 }}>{index + 1}. {step.title}</h3>
              <p className="muted">{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="card card-flat">
        <div className="between">
          <div className="stack-sm" style={{ maxWidth: '36rem' }}>
            <h2 className="section-title">A worked example</h2>
            <p className="muted">
              Ten members put in ₹5,000 each, so the pot is ₹50,000. Asha bids ₹41,000 and wins it. The ₹9,000 she left is shared
              by the other nine, ₹1,000 each. Next month Asha sits out and the rest bid. After ten rounds everyone has won once.
            </p>
          </div>
          <Link href="/register" className="btn btn-primary">Try it now</Link>
        </div>
      </section>
    </div>
  );
}
