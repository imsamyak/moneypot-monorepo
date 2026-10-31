import Link from 'next/link';
import { SdkHttpError, type PotResponse } from '@moneypot/platform-sdk';
import { redirect } from 'next/navigation';
import { Countdown } from '@/components/Countdown';
import { Icon } from '@/components/icons';
import { JoinWithCode } from '@/components/JoinWithCode';
import { PotArt } from '@/components/PotArt';
import { SubmitButton } from '@/components/SubmitButton';
import { joinByCode } from '@/lib/actions';
import { formatDateTime, formatDuration, formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

/** What an invite code opens: the pot, and the button that joins it (and pays). */
export default async function JoinPage({
  params,
  searchParams,
}: {
  params: Promise<{ code: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const { code } = await params;
  const { error } = await searchParams;
  const user = await requireUser(`/join/${code}`);

  let pot: PotResponse | null = null;
  try {
    pot = await api().pots.getPotByInvite({ inviteCode: code });
  } catch (e) {
    if (!(e instanceof SdkHttpError && (e.status === 404 || e.status === 400))) throw e;
  }

  if (!pot) {
    return (
      <div className="card stack" style={{ maxWidth: '34rem', margin: '1rem auto', textAlign: 'center' }}>
        <div style={{ justifySelf: 'center', opacity: 0.5 }}><PotArt level={0.06} size={110} /></div>
        <h1 style={{ fontSize: '1.6rem', fontWeight: 850 }}>That code does not match a pot</h1>
        <p className="muted">Check it for typos, or ask the owner to send it again.</p>
        <JoinWithCode initial={code} />
      </div>
    );
  }
  if (pot.members.some((m) => m.userId === user.id)) redirect(`/pots/${pot.id}`);

  const wallet = await api().wallet.openWallet();
  const open = pot.status === 'OPEN';
  const enough = wallet.balance >= pot.contribution;

  return (
    <div className="stack" style={{ maxWidth: '44rem', margin: '0 auto' }}>
      <section className="hero-card">
        <div className="between" style={{ flexWrap: 'nowrap', alignItems: 'center' }}>
          <div className="stack-sm">
            <span className="chip chip-glass" style={{ justifySelf: 'start' }}><Icon name="gift" size={14} /> You are invited</span>
            <h1 style={{ fontSize: '2rem', fontWeight: 850, lineHeight: 1.1 }}>{pot.name}</h1>
            <p className="muted-on">{pot.memberCount} {pot.memberCount === 1 ? 'person has' : 'people have'} joined so far.</p>
          </div>
          <PotArt level={Math.min(1, 0.15 + (pot.memberCount / 10) * 0.85)} size={110} />
        </div>
        <div className="stat-strip">
          <div className="stat"><div className="stat-label">You pay</div><div className="stat-value">{formatMoney(pot.contribution)}</div></div>
          <div className="stat"><div className="stat-label">Bidding starts</div><div className="stat-value" style={{ fontSize: '1rem' }}>{open ? <Countdown endsAt={pot.startsAt} doneText="Starting now" suffix="" /> : 'Started'}</div></div>
          <div className="stat"><div className="stat-label">Each round open</div><div className="stat-value" style={{ fontSize: '1rem' }}>{formatDuration(pot.bidWindowSeconds)}</div></div>
        </div>
      </section>

      {error && <p className="notice notice-error" role="alert">{error}</p>}

      {!open ? (
        <p className="notice notice-error">Joining is closed: bidding has already started or the pot is over.</p>
      ) : (
        <div className="card stack">
          <p>
            Joining takes <strong>{formatMoney(pot.contribution)}</strong> from your wallet right away. Bidding starts{' '}
            <strong>{formatDateTime(pot.startsAt)}</strong>, and until then you can leave and get it all back.
          </p>
          <p className="muted small">Your wallet balance: {formatMoney(wallet.balance)}</p>
          {pot.joinProblem && <p className="notice notice-error">Your last payment did not go through: {pot.joinProblem}.</p>}
          {!enough && (
            <p className="notice notice-error">
              Your wallet is short by {formatMoney(pot.contribution - wallet.balance)}. <Link href="/wallet" style={{ fontWeight: 700 }}>Add money to your wallet</Link> first.
            </p>
          )}
          <form action={joinByCode}>
            <input type="hidden" name="inviteCode" value={code} />
            <SubmitButton>Join and pay {formatMoney(pot.contribution)}</SubmitButton>
          </form>
        </div>
      )}
    </div>
  );
}
