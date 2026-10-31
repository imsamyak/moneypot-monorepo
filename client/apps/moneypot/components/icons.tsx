const PATHS = {
  coin: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M14.8 9.2c-.5-.8-1.5-1.2-2.8-1.2-1.6 0-2.7.8-2.7 2 0 3 5.6 1.3 5.6 4.2 0 1.2-1.2 2-2.9 2-1.3 0-2.4-.5-2.9-1.4', 'M12 6.5V8m0 8v1.5'],
  wallet: ['M3 7.5A2.5 2.5 0 0 1 5.5 5H18a1 1 0 0 1 1 1v2', 'M3 7.5V17a2 2 0 0 0 2 2h14a1 1 0 0 0 1-1v-9a1 1 0 0 0-1-1H5.5A2.5 2.5 0 0 1 3 7.5Z', 'M16 13.5h.01'],
  users: ['M16 19v-1.5a3.5 3.5 0 0 0-3.5-3.5h-4A3.5 3.5 0 0 0 5 17.5V19', 'M10.5 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z', 'M19 19v-1a3 3 0 0 0-2.2-2.9', 'M15.5 5.2a3 3 0 0 1 0 5.6'],
  trophy: ['M8 4h8v5a4 4 0 0 1-8 0V4Z', 'M8 6H5v1.5A3.5 3.5 0 0 0 8.5 11', 'M16 6h3v1.5a3.5 3.5 0 0 1-3.5 3.5', 'M12 13v4m-3 3h6m-5-3h4'],
  down: ['M12 5v14', 'm6 13 6 6 6-6'],
  up: ['M12 19V5', 'm6 11 6-6 6 6'],
  gift: ['M4 11h16v9H4v-9Z', 'M3 8h18v3H3V8Z', 'M12 8v12', 'M12 8c-1-3-4.5-3.5-4.5-1.5S10 8 12 8Zm0 0c1-3 4.5-3.5 4.5-1.5S14 8 12 8Z'],
  clock: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18Z', 'M12 7v5l3 2'],
  check: ['m5 12.5 4.5 4.5L19 7.5'],
  cross: ['M6 6l12 12M18 6 6 18'],
  bolt: ['M13 3 5 13.5h6L10 21l8-10.5h-6L13 3Z'],
  plus: ['M12 5v14M5 12h14'],
  gavel: ['m14 4 6 6', 'm11 7 6 6', 'm5 19 7-7', 'M3 21h9'],
  pot: ['M5 10h14l-1 8a3 3 0 0 1-3 2.5H9A3 3 0 0 1 6 18l-1-8Z', 'M4 10h16', 'M9 10V8a3 3 0 0 1 6 0v2'],
} as const;

export type IconName = keyof typeof PATHS;

export function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
