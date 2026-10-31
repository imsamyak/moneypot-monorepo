const COLORS = ['#ffc439', '#009cde', '#0070ba', '#ffffff', '#7cc4ff', '#ff8a5c'];

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
