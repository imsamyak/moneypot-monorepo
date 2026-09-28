import type { Metadata } from 'next';
import Link from 'next/link';
import { logout } from '@/lib/actions';
import { getUser } from '@/lib/session';
import './globals.css';

export const metadata: Metadata = {
  title: 'MoneyPot',
  description: 'Save together. Bid for the pot, lowest bid wins, and the rest comes back to the group.',
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const user = await getUser();
  return (
    <html lang="en">
      <body>
        <header style={{ borderBottom: '1px solid var(--line)', background: 'var(--card)' }}>
          <div className="page" style={{ paddingTop: '0.9rem', paddingBottom: '0.9rem', display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            <Link href="/" style={{ fontWeight: 800, fontSize: '1.2rem', color: 'var(--brand)' }}>
              MoneyPot
            </Link>
            {user && (
              <nav style={{ display: 'flex', gap: '1.1rem', fontWeight: 600 }}>
                <Link href="/pots">Pots</Link>
                <Link href="/wallet">Wallet</Link>
              </nav>
            )}
            <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
              {user ? (
                <>
                  <span className="muted" style={{ fontSize: '0.9rem' }}>{user.email}</span>
                  <form action={logout}>
                    <button className="btn" type="submit">Sign out</button>
                  </form>
                </>
              ) : (
                <>
                  <Link href="/login" className="btn">Sign in</Link>
                  <Link href="/register" className="btn btn-primary">Create account</Link>
                </>
              )}
            </div>
          </div>
        </header>
        <main className="page">{children}</main>
      </body>
    </html>
  );
}
