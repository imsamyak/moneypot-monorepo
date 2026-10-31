import Link from 'next/link';
import { DepositForm } from '@/components/DepositForm';
import { Icon, type IconName } from '@/components/icons';
import { formatDateTime, formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const PAGE_SIZE = 12;

const KIND = {
  DEPOSIT: { label: 'Top-up', icon: 'plus', tone: '' },
  CONTRIBUTION: { label: 'Paid into a pot', icon: 'up', tone: 'icon-bubble-bad' },
  PAYOUT: { label: 'Collected the pot', icon: 'trophy', tone: 'icon-bubble-gold' },
  DIVIDEND: { label: 'Share of what the collector left', icon: 'gift', tone: '' },
  REFUND: { label: 'Refund from a pot', icon: 'down', tone: '' },
} as const satisfies Record<string, { label: string; icon: IconName; tone: string }>;

export default async function WalletPage({ searchParams }: { searchParams: Promise<{ page?: string }> }) {
  await requireUser('/wallet');
  const page = Math.max(0, Number((await searchParams).page ?? 0) || 0);
  // opening is idempotent: the first visit creates the wallet, later ones just read it
  const wallet = await api().wallet.openWallet();
  const ledger = await api().wallet.getLedger({ page, size: PAGE_SIZE });

  return (
    <div className="stack">
      <div className="hero-card">
        <div className="between">
          <div>
            <p className="muted-on cluster" style={{ gap: '0.4rem' }}><Icon name="wallet" size={18} /> Wallet balance</p>
            <p className="big-number" style={{ marginTop: '0.3rem' }}>{formatMoney(wallet.balance)}</p>
          </div>
          <Link href="/pots" className="btn btn-glass">Go to pots</Link>
        </div>
      </div>

      <div className="two-col">
        <div className="card stack-sm">
          <h2 className="section-title">History</h2>
          {ledger.items.length === 0 && <p className="muted">Nothing yet. Add some money to get started.</p>}
          <div>
            {ledger.items.map((entry) => {
              const kind = KIND[entry.kind];
              return (
                <div className="row" key={entry.id}>
                  <div className="cluster" style={{ flexWrap: 'nowrap', gap: '0.8rem' }}>
                    <span className={`icon-bubble ${kind.tone}`}><Icon name={kind.icon} /></span>
                    <div>
                      <div style={{ fontWeight: 700 }}>{kind.label}</div>
                      <div className="muted small">{formatDateTime(entry.createdAt)}</div>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: 800, color: entry.type === 'CREDIT' ? 'var(--brand)' : 'var(--ink)' }}>
                      {entry.type === 'CREDIT' ? '+' : '-'}{formatMoney(entry.amount)}
                    </div>
                    <div className="muted small">Balance {formatMoney(entry.balanceAfter)}</div>
                  </div>
                </div>
              );
            })}
          </div>
          <div className="cluster">
            {page > 0 && <Link className="btn" href={`/wallet?page=${page - 1}`}>← Newer</Link>}
            {ledger.nextPage !== null && <Link className="btn" href={`/wallet?page=${ledger.nextPage}`}>Older →</Link>}
          </div>
        </div>

        <div className="card stack">
          <h2 className="section-title">Add money</h2>
          <DepositForm />
        </div>
      </div>
    </div>
  );
}
