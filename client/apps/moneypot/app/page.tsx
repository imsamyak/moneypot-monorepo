import Link from 'next/link';
import { redirect } from 'next/navigation';
import { Avatar } from '@/components/Avatar';
import { Calculator } from '@/components/Calculator';
import { Icon, type IconName } from '@/components/icons';
import { PotArt } from '@/components/PotArt';
import { getUser } from '@/lib/session';

const STEPS: { icon: IconName; title: string; text: string }[] = [
  { icon: 'users', title: 'Invite your group', text: 'Create a pot, choose when bidding starts, and share the private invite code. There is no limit on members.' },
  { icon: 'wallet', title: 'Pay to join', text: 'Your contribution goes in when you join. Until bidding starts you can still leave and get it back.' },
  { icon: 'gavel', title: 'Bid for the pot', text: 'At the start time joining closes and bidding opens. Members who have not collected yet say the least they will accept. The lowest bid collects the pot.' },
  { icon: 'gift', title: 'Share the rest', text: 'What the collector leaves behind goes back to everyone else, straight to their wallets.' },
];

export default async function Home() {
  if (await getUser()) redirect('/pots');
  return (
    <div className="stack" style={{ gap: '4.5rem' }}>
      <section className="hero-landing">
        <div className="stack">
          <span className="chip" style={{ justifySelf: 'start' }}><Icon name="bolt" size={14} /> Save together</span>
          <h1 className="hero-title">
            Bid for the pot. <span className="gradient-text">Lowest bid collects.</span>
          </h1>
          <p className="muted" style={{ fontSize: '1.15rem', maxWidth: '34rem' }}>
            Save with people you trust. Start a pot, invite friends with a private code, and everyone pays in. When bidding opens,
            whoever will settle for the least collects the pot, and the difference is shared back with the rest of the group.
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
          <div className="between" style={{ marginTop: '0.6rem', flexWrap: 'nowrap' }}>
            <div>
              <p className="muted-on">The pot</p>
              <p className="big-number">₹50,000</p>
            </div>
            <PotArt level={0.85} size={120} />
          </div>
          <div className="stack-sm" style={{ marginTop: '0.8rem' }}>
            {[
              { order: 4, amount: '₹41,000', width: 82, lead: true },
              { order: 7, amount: '₹44,500', width: 89, lead: false },
              { order: 2, amount: '₹47,000', width: 94, lead: false },
            ].map((bid) => (
              <div key={bid.order} style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', alignItems: 'center', gap: '0.7rem' }}>
                <Avatar order={bid.order} size={30} />
                <div className="track" style={{ background: 'rgba(255,255,255,0.2)' }}>
                  <div className="track-fill" style={{ width: `${bid.width}%`, background: bid.lead ? '#ffc439' : 'rgba(255,255,255,0.75)' }} />
                </div>
                <strong>{bid.amount}</strong>
              </div>
            ))}
          </div>
          <p className="muted-on small" style={{ marginTop: '0.9rem' }}>
            Member 4 leads and would collect ₹41,000. The other ₹9,000 would be shared by the nine others.
          </p>
        </div>
      </section>

      <section className="stack">
        <div className="stack-sm">
          <span className="eyebrow">How it works</span>
          <h2 style={{ fontSize: '2rem', fontWeight: 850 }}>Four steps, no spreadsheets</h2>
        </div>
        <div className="steps-grid">
          {STEPS.map((step, index) => (
            <div className="card stack-sm card-link" key={step.title} style={{ animationDelay: `${index * 80}ms` }}>
              <div className="between" style={{ flexWrap: 'nowrap' }}>
                <span className="icon-bubble"><Icon name={step.icon} /></span>
                <span className="num-badge">{index + 1}</span>
              </div>
              <h3 style={{ fontWeight: 800 }}>{step.title}</h3>
              <p className="muted">{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="stack">
        <div className="stack-sm">
          <span className="eyebrow">Try the maths</span>
          <h2 style={{ fontSize: '2rem', fontWeight: 850 }}>See who gets what</h2>
        </div>
        <Calculator />
      </section>

      <section className="hero-card cta-band">
        <h2 style={{ fontSize: '2rem', fontWeight: 850 }}>Ready to start a pot?</h2>
        <p className="muted-on" style={{ margin: '0.5rem 0 1.4rem' }}>Create one in a minute, then send your friends the code.</p>
        <Link href="/register" className="btn btn-gold btn-lg">Get started free</Link>
      </section>
    </div>
  );
}
