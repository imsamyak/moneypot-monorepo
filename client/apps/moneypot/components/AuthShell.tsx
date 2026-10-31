import { Icon } from './icons';
import { LogoMark } from './Logo';

/** The two-column frame of the sign-in and sign-up pages: a brand panel and the form. */
export function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <div className="auth-shell rise">
      <aside className="auth-side">
        <div className="cluster" style={{ gap: '0.7rem' }}>
          <LogoMark size={40} />
          <strong style={{ fontSize: '1.3rem' }}>MoneyPot</strong>
        </div>
        <div className="stack-sm">
          <h2 style={{ fontSize: '1.7rem', fontWeight: 800, lineHeight: 1.15 }}>Save together. The lowest bid collects the pot.</h2>
          <p className="muted-on">Everyone pays in, members bid, and what is left is shared back with the group.</p>
        </div>
        <ul className="stack-sm" style={{ listStyle: 'none', padding: 0 }}>
          {['Join by invitation: your money goes in when you join', 'Leave before bidding starts and get it all back', 'Updates appear live while a round runs'].map((line) => (
            <li key={line} className="cluster" style={{ flexWrap: 'nowrap', alignItems: 'flex-start' }}>
              <span style={{ marginTop: 2 }}><Icon name="check" size={18} /></span>
              <span className="muted-on small">{line}</span>
            </li>
          ))}
        </ul>
      </aside>
      <div className="auth-form stack">
        <div>
          <h1 style={{ fontSize: '1.7rem', fontWeight: 800 }}>{title}</h1>
          <p className="muted">{subtitle}</p>
        </div>
        {children}
      </div>
    </div>
  );
}
