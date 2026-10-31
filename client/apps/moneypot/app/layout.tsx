import type { Metadata } from 'next';
import { logout } from '@/lib/actions';
import { getUser } from '@/lib/session';
import { Logo } from '@/components/Logo';
import { NavLink } from '@/components/NavLink';
import Link from 'next/link';
import './globals.css';

export const metadata: Metadata = {
  title: 'MoneyPot',
  description: 'Save with people you trust. Bid for the pot, the lowest bid collects it, and the rest comes back to the group.',
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const user = await getUser();
  return (
    <html lang="en">
      <body>
        <header className="header">
          <div className="header-inner">
            <Logo />
            {user && (
              <nav className="nav">
                <NavLink href="/pots">Pots</NavLink>
                <NavLink href="/wallet">Wallet</NavLink>
              </nav>
            )}
            <div className="who">
              {user ? (
                <>
                  <span className="who-email">{user.email}</span>
                  <form action={logout}>
                    <button className="btn" type="submit">Sign out</button>
                  </form>
                </>
              ) : (
                <>
                  <Link href="/login" className="btn">Sign in</Link>
                  <Link href="/register" className="btn btn-primary">Get started</Link>
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
