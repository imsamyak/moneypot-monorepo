'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Icon } from './icons';

/** Where a member types the invite code they were given. */
export function JoinWithCode({ initial = '' }: { initial?: string }) {
  const router = useRouter();
  const [code, setCode] = useState(initial);

  function find(event: React.FormEvent) {
    event.preventDefault();
    const clean = code.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
    if (clean) router.push(`/join/${clean}`);
  }

  return (
    <form onSubmit={find} className="cluster" style={{ flexWrap: 'nowrap' }}>
      <input
        className="input code-input"
        aria-label="Invite code"
        placeholder="Paste an invite code"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        autoComplete="off"
        spellCheck={false}
      />
      <button type="submit" className="btn btn-primary"><Icon name="gift" size={16} /> Find pot</button>
    </form>
  );
}
