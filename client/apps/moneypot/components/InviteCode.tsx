'use client';

import { useState } from 'react';
import { Icon } from './icons';

/** The invite code of a pot, big and easy to read out, with buttons to copy it or a ready-to-send message. */
export function InviteCode({ code, name }: { code: string; name: string }) {
  const [copied, setCopied] = useState<'code' | 'message' | null>(null);
  const groups = code.match(/.{1,4}/g) ?? [code];

  async function copy(text: string, what: 'code' | 'message') {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(what);
      setTimeout(() => setCopied(null), 2000);
    } catch {
      // clipboard access can be refused; the code is on screen, so it can be copied by hand
    }
  }

  return (
    <div className="stack-sm">
      <div className="code-row" aria-label={`Invite code ${code}`}>
        {groups.map((group, i) => (
          <span className="code-group" key={i}>{group}</span>
        ))}
      </div>
      <div className="cluster">
        <button type="button" className="btn btn-primary" onClick={() => copy(code, 'code')}>
          <Icon name={copied === 'code' ? 'check' : 'plus'} size={16} /> {copied === 'code' ? 'Copied' : 'Copy code'}
        </button>
        <button
          type="button"
          className="btn"
          onClick={() => copy(`Join my MoneyPot "${name}". Open ${window.location.origin}/pots, choose "Join with a code" and enter ${code}`, 'message')}
        >
          <Icon name={copied === 'message' ? 'check' : 'gift'} size={16} /> {copied === 'message' ? 'Copied' : 'Copy a message to send'}
        </button>
      </div>
    </div>
  );
}
