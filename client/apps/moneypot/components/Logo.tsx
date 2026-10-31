import Link from 'next/link';

/** The MoneyPot mark: a pot, flat, in the brand navy. */
export function LogoMark({ size = 30 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" aria-hidden="true">
      <rect width="40" height="40" rx="8" fill="#003087" />
      <path d="M9 17h22l-1.8 12.5a4 4 0 0 1-4 3.5H14.8a4 4 0 0 1-4-3.5L9 17Z" fill="#ffffff" />
      <rect x="8" y="14.5" width="24" height="3.5" rx="1.75" fill="#7cc4ff" />
      <circle cx="20" cy="10" r="3.2" fill="#ffc439" />
    </svg>
  );
}

export function Logo() {
  return (
    <Link href="/" className="logo">
      <LogoMark />
      <span>
        Money<span style={{ color: 'var(--brand)' }}>Pot</span>
      </span>
    </Link>
  );
}
