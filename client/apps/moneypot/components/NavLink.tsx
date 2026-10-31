'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

/** A header link that shows when it is the page you are on. */
export function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
  const pathname = usePathname();
  const active = pathname === href || pathname.startsWith(`${href}/`);
  return (
    <Link href={href} className="nav-link" data-active={active}>
      {children}
    </Link>
  );
}
