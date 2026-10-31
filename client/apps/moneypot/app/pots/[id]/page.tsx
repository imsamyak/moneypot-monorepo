import Link from 'next/link';
import { SdkHttpError, type PotResponse } from '@moneypot/platform-sdk';
import { notFound } from 'next/navigation';
import { MemberTag } from '@/components/Avatar';
import { Countdown } from '@/components/Countdown';
import { InviteCode } from '@/components/InviteCode';
import { LiveUpdates } from '@/components/LiveUpdates';
import { PotHistory } from '@/components/PotHistory';
import { PotRound } from '@/components/PotRound';
import { SubmitButton } from '@/components/SubmitButton';
import { leavePot } from '@/lib/actions';
import { formatDateTime, formatDuration, formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const STATUS = {
  OPEN: { label: 'Joining open', chip: 'chip chip-warn' },
  ACTIVE: { label: 'Bidding', chip: 'chip' },
  COMPLETED: { label: 'Completed', chip: 'chip chip-muted' },
  CANCELLED: { label: 'Cancelled', chip: 'chip chip-bad' },
} as const;

export default async function PotPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string }>;
}) {
  const { id } = await params;
  const { error } = await searchParams;
  const user = await requireUser(`/pots/${id}`);

  let pot: PotResponse;
  try {
    pot = await api().pots.getPot({ potId: id });
  } catch (e) {
    if (e instanceof SdkHttpError && (e.status === 404 || e.status === 400)) notFound();
    throw e;
  }

  const mine = pot.members.find((m) => m.userId === user.id);
  const isOwner = pot.ownerId === user.id;
  const open = pot.status === 'OPEN';
  const live = Boolean(mine) && (pot.status === 'OPEN' || pot.status === 'ACTIVE');
  const status = STATUS[pot.status];

  return (
    <div className="stack">
      {live && <LiveUpdates potId={pot.id} />}
      {error && <p className="notice notice-error" role="alert">{error}</p>}

      <section className="card">
        <div className="between" style={{ alignItems: 'flex-start' }}>
          <div className="stack-sm">
            <div className="cluster">
              <span className={status.chip}>{status.label}</span>
              {pot.demo && <span className="chip chip-warn">Demo</span>}
              {live && !pot.demo && <span className="live">Live</span>}
            </div>
            <h1 style={{ fontSize: '1.6rem', fontWeight: 700 }}>{pot.name}</h1>
            <p className="muted">{formatMoney(pot.contribution)} from each member. The lowest bid collects the pot, and what is left is shared.</p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p className="muted small">{open ? 'In the pot so far' : 'The pot'}</p>
            <p className="big-number">{formatMoney(pot.potAmount)}</p>
          </div>
        </div>
        <div className="stat-strip">
          <div className="stat">
            <div className="stat-label">{open ? 'Joined' : 'Members'}</div>
            <div className="stat-value">{pot.memberCount}</div>
          </div>
          <div className="stat">
            <div className="stat-label">{open ? 'Bidding starts' : 'Lowest bid allowed'}</div>
            <div className="stat-value" style={{ fontSize: '1rem' }}>
              {open ? <Countdown endsAt={pot.startsAt} doneText="Starting now" suffix="" /> : formatMoney(pot.minBid)}
            </div>
          </div>
          <div className="stat">
            <div className="stat-label">Bidding open for</div>
            <div className="stat-value" style={{ fontSize: '1rem' }}>{formatDuration(pot.bidWindowSeconds)}</div>
          </div>
        </div>
      </section>

      {pot.demo && (
        <p className="notice">
          This is a demo pot. The other four members are simulated and no real money moves, so your wallet is not used. Bid, end the
          bidding, and watch the rounds go by.
        </p>
      )}

      {open && (
        <div className="card stack">
          <div>
            <div style={{ fontWeight: 700 }}>Bidding starts {formatDateTime(pot.startsAt)}</div>
            <div className="muted small">Until then people can join, and members can leave for a full refund. After that nobody can.</div>
          </div>

          {!mine && (
            <p className="notice">This pot is by invitation only. Ask its owner for the invite code, then enter it under{' '}
              <Link href="/pots" style={{ fontWeight: 700 }}>Join with a code</Link>.</p>
          )}

          {mine?.status === 'PENDING' && <p className="notice">Your payment of {formatMoney(pot.contribution)} is being processed...</p>}
          {mine?.status === 'ACTIVE' && <p className="notice">You are in: your {formatMoney(pot.contribution)} is in the pot.</p>}

          {isOwner && pot.inviteCode && (
            <div className="stack-sm">
              <p className="label">Invite people with this code</p>
              <InviteCode code={pot.inviteCode} name={pot.name} />
            </div>
          )}

          {mine?.status === 'ACTIVE' && !isOwner && (
            <form action={leavePot}>
              <input type="hidden" name="potId" value={pot.id} />
              <SubmitButton primary={false}>Leave and get {formatMoney(pot.contribution)} back</SubmitButton>
            </form>
          )}
        </div>
      )}

      {pot.status === 'CANCELLED' && (
        <p className="notice notice-error">
          Fewer than two members had joined by the start time, so this pot was cancelled and everyone was refunded.
        </p>
      )}
      {pot.status === 'COMPLETED' && <p className="notice">Every member has collected once. This pot is complete.</p>}

      {pot.round && mine && pot.status === 'ACTIVE' && <PotRound pot={pot} round={pot.round} me={user.id} isOwner={isOwner} />}
      {pot.round && !mine && pot.status === 'ACTIVE' && (
        <p className="notice">Bidding has started and the group is fixed. Only members can see the round.</p>
      )}

      <PotHistory pot={pot} me={user.id} />

      <div className="card stack-sm">
        <h2 className="section-title">Members</h2>
        {pot.members.length === 0 && <p className="muted">Nobody has joined yet.</p>}
        <div>
          {pot.members.map((member) => (
            <div className="row" key={member.userId}>
              <span className="cluster" style={{ gap: '0.5rem' }}>
                <MemberTag userId={member.userId} members={pot.members} me={user.id} />
                {member.userId === pot.ownerId && <span className="chip chip-warn">owner</span>}
              </span>
              {open ? (
                <span className={member.status === 'ACTIVE' ? 'chip' : 'chip chip-warn'}>{member.status === 'ACTIVE' ? 'Paid' : 'Paying...'}</span>
              ) : (
                <span className={member.won ? 'chip' : 'chip chip-muted'}>{member.won ? `collected in round ${member.wonRound}` : 'yet to collect'}</span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
