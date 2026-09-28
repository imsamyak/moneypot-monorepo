import Link from 'next/link';
import { ActionForm, Field } from '@/components/ActionForm';
import { deposit } from '@/lib/actions';
import { formatDateTime, formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const PAGE_SIZE = 15;

const KIND_LABEL = {
  DEPOSIT: 'Top-up',
  CONTRIBUTION: 'Paid into a pot',
  PAYOUT: 'Won the pot',
  DIVIDEND: 'Share of what the winner left',
} as const;

export default async function WalletPage({ searchParams }: { searchParams: Promise<{ page?: string }> }) {
  await requireUser('/wallet');
  const page = Math.max(0, Number((await searchParams).page ?? 0) || 0);
  // opening is idempotent: the first visit creates the wallet, later ones just read it
  const wallet = await api().wallet.openWallet();
  const ledger = await api().wallet.getLedger({ page, size: PAGE_SIZE });

  return (
    <div style={{ display: 'grid', gap: '1.2rem' }}>
      <div className="card">
        <p className="muted">Wallet balance</p>
        <p style={{ fontSize: '2.2rem', fontWeight: 800 }}>{formatMoney(wallet.balance)}</p>
      </div>

      <div className="card">
        <h2 style={{ fontWeight: 700, marginBottom: '0.8rem' }}>Add money</h2>
        <ActionForm action={deposit} submitLabel="Add to wallet">
          <Field label="Amount (₹)" name="amount" inputMode="decimal" placeholder="5000" hint="Up to ₹1,00,000 at a time. This is a test top-up." required />
        </ActionForm>
      </div>

      <div className="card">
        <h2 style={{ fontWeight: 700, marginBottom: '0.4rem' }}>History</h2>
        {ledger.items.length === 0 && <p className="muted">Nothing yet. Add some money to get started.</p>}
        {ledger.items.map((entry) => (
          <div className="row" key={entry.id}>
            <div>
              <div style={{ fontWeight: 600 }}>{KIND_LABEL[entry.kind]}</div>
              <div className="muted" style={{ fontSize: '0.8rem' }}>{formatDateTime(entry.createdAt)}</div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontWeight: 700, color: entry.type === 'CREDIT' ? 'var(--brand)' : 'var(--ink)' }}>
                {entry.type === 'CREDIT' ? '+' : '-'}
                {formatMoney(entry.amount)}
              </div>
              <div className="muted" style={{ fontSize: '0.8rem' }}>Balance {formatMoney(entry.balanceAfter)}</div>
            </div>
          </div>
        ))}
        <div style={{ display: 'flex', gap: '0.6rem', marginTop: '1rem' }}>
          {page > 0 && <Link className="btn" href={`/wallet?page=${page - 1}`}>Newer</Link>}
          {ledger.nextPage !== null && <Link className="btn" href={`/wallet?page=${ledger.nextPage}`}>Older</Link>}
        </div>
      </div>
    </div>
  );
}
