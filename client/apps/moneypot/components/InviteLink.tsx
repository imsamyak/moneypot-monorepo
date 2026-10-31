'use client';

import { useEffect, useState } from 'react';
import { Icon } from './icons';

/** The private link that lets someone join the pot, with a copy button. */
export function InviteLink({ potId, code }: { potId: string; code: string }) {
  const [link, setLink] = useState('');
  const [copied, setCopied] = useState(false);
  useEffect(() => setLink(`${window.location.origin}/pots/${potId}?code=${code}`), [potId, code]);

  async function copy() {
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // clipboard access can be refused; the link is shown, so it can be copied by hand
    }
  }

  return (
    <div className="stack-sm">
      <div className="cluster" style={{ flexWrap: 'nowrap' }}>
        <input className="input" readOnly value={link} aria-label="Invite link" onFocus={(e) => e.currentTarget.select()} />
        <button type="button" className="btn btn-primary" onClick={copy}>
          <Icon name={copied ? 'check' : 'plus'} size={16} /> {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
    </div>
  );
}
