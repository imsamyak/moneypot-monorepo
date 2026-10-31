'use client';

import { useId } from 'react';

/**
 * The pot: a golden pot that fills as money goes in, with coins dropping into it. {@code level} is 0 to 1, how full it is.
 * The animation is CSS, so it costs nothing and stops for people who asked for less motion.
 */
export function PotArt({ level = 0.5, size = 160 }: { level?: number; size?: number }) {
  const id = useId().replace(/:/g, '');
  const top = 178 - Math.max(0.06, Math.min(1, level)) * 84;
  return (
    <svg width={size} height={size} viewBox="0 0 200 200" role="img" aria-label="A pot filling with coins" style={{ flex: 'none' }}>
      <defs>
        <clipPath id={`${id}-clip`}>
          <path d="M38 92h124l-9 66a24 24 0 0 1-24 21H71a24 24 0 0 1-24-21L38 92Z" />
        </clipPath>
        <linearGradient id={`${id}-body`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="1" stopColor="#c7ddf2" />
        </linearGradient>
        <linearGradient id={`${id}-gold`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#ffe08a" />
          <stop offset="1" stopColor="#f5b800" />
        </linearGradient>
      </defs>
      <ellipse cx="100" cy="186" rx="62" ry="7" fill="rgba(0,0,0,0.18)" />
      <g>
        <circle className="coin" cx="84" cy="30" r="11" fill="#ffc439" stroke="#d99a00" strokeWidth="3" />
        <circle className="coin coin-2" cx="116" cy="26" r="11" fill="#ffd966" stroke="#d99a00" strokeWidth="3" />
        <circle className="coin coin-3" cx="100" cy="22" r="11" fill="#ffc439" stroke="#d99a00" strokeWidth="3" />
      </g>
      <path d="M38 92h124l-9 66a24 24 0 0 1-24 21H71a24 24 0 0 1-24-21L38 92Z" fill={`url(#${id}-body)`} />
      <g clipPath={`url(#${id}-clip)`}>
        <rect x="0" y="0" width="200" height="130" fill={`url(#${id}-gold)`} style={{ transform: `translateY(${top}px)`, transition: 'transform 0.9s cubic-bezier(0.2, 0.8, 0.2, 1)' }} />
        <ellipse cx="78" cy="130" rx="10" ry="30" fill="rgba(255,255,255,0.35)" transform="rotate(12 78 130)" />
      </g>
      <rect x="28" y="82" width="144" height="16" rx="8" fill="#ffffff" />
      <path d="M40 110c-18 0-18 28 0 28M160 110c18 0 18 28 0 28" fill="none" stroke="#ffffff" strokeWidth="8" strokeLinecap="round" />
    </svg>
  );
}
