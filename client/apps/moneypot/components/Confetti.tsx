const COLORS = ['#fbbf24', '#2dd4bf', '#f472b6', '#60a5fa', '#a78bfa', '#fb7185'];

/** A burst of falling confetti behind a celebration. Pure CSS, positioned over the nearest relative parent. */
export function Confetti({ pieces = 28 }: { pieces?: number }) {
  return (
    <div className="confetti" aria-hidden="true">
      {Array.from({ length: pieces }, (_, i) => (
        <i
          key={i}
          style={
            {
              '--x': `${(i * 37) % 100}%`,
              '--d': `${(i % 9) * 0.28}s`,
              '--r': `${(i * 53) % 360}deg`,
              '--c': COLORS[i % COLORS.length],
            } as React.CSSProperties
          }
        />
      ))}
    </div>
  );
}
