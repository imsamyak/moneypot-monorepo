import Link from 'next/link';

/** The MoneyPot mark: a coin dropping into a pot. */
export function LogoMark({ size = 32 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" aria-hidden="true">
      <defs>
        <linearGradient id="mp-g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#0d9488" />
          <stop offset="1" stopColor="#2cc9b6" />
        </linearGradient>
      </defs>
      <rect width="40" height="40" rx="11" fill="url(#mp-g)" />
      <circle cx="20" cy="12" r="5" fill="#fbbf24" />
      <path d="M9 19h22l-1.8 11a4 4 0 0 1-4 3.4H14.8a4 4 0 0 1-4-3.4L9 19Z" fill="#fff" />
      <rect x="8" y="17.5" width="24" height="3.5" rx="1.75" fill="#d6f2ec" />
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
