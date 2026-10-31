import Link from 'next/link';
import { SdkHttpError, type PotResponse } from '@moneypot/platform-sdk';
import { notFound } from 'next/navigation';
import { MemberTag } from '@/components/Avatar';
import { Countdown } from '@/components/Countdown';
import { Icon } from '@/components/icons';
import { InviteLink } from '@/components/InviteLink';
import { LiveUpdates } from '@/components/LiveUpdates';
import { PotHistory } from '@/components/PotHistory';
import { PotRound } from '@/components/PotRound';
import { SubmitButton } from '@/components/SubmitButton';
import { joinPot, leavePot } from '@/lib/actions';
import { formatDateTime, formatDuration, formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const STATUS = {
  OPEN: { label: 'Joining open', chip: 'chip chip-glass' },
  ACTIVE: { label: 'Bidding', chip: 'chip chip-glass' },
  COMPLETED: { label: 'Completed', chip: 'chip chip-glass' },
  CANCELLED: { label: 'Cancelled', chip: 'chip chip-glass' },
} as const;

export default async function PotPage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ error?: string; code?: string }>;
}) {
  const { id } = await params;
  const { error, code } = await searchParams;
  const user = await requireUser(code ? `/pots/${id}?code=${encodeURIComponent(code)}` : `/pots/${id}`);

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

      <section className="hero-card">
        <div className="between" style={{ alignItems: 'flex-start' }}>
          <div className="stack-sm">
            <div className="cluster">
              <span className={status.chip}>{status.label}</span>
              {live && <span className="live">Live</span>}
            </div>
            <h1 style={{ fontSize: '1.9rem', fontWeight: 850, lineHeight: 1.1 }}>{pot.name}</h1>
            <p className="muted-on">
              {formatMoney(pot.contribution)} from each member. The lowest bid wins, and what is left is shared.
            </p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p className="muted-on">{open ? 'In the pot so far' : 'The pot'}</p>
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

      {open && (
        <div className="card stack">
          <div className="cluster" style={{ gap: '0.7rem' }}>
            <span className="icon-bubble"><Icon name="clock" /></span>
            <div>
              <div style={{ fontWeight: 800 }}>Bidding starts {formatDateTime(pot.startsAt)}</div>
              <div className="muted small">Until then people can join, and members can leave for a full refund. After that nobody can.</div>
            </div>
          </div>

          {!mine && (
            code ? (
              <div className="stack-sm">
                <p>You are invited to this pot. Joining takes <strong>{formatMoney(pot.contribution)}</strong> from your wallet right away.</p>
                {pot.joinProblem && (
                  <p className="notice notice-error">
                    Your last payment did not go through: {pot.joinProblem}. <Link href="/wallet" style={{ fontWeight: 700 }}>Add money to your wallet</Link> and try again.
                  </p>
                )}
                <form action={joinPot} className="cluster">
                  <input type="hidden" name="potId" value={pot.id} />
                  <input type="hidden" name="inviteCode" value={code} />
                  <SubmitButton>Join and pay {formatMoney(pot.contribution)}</SubmitButton>
                </form>
              </div>
            ) : (
              <p className="notice">This pot is by invitation only. Ask its owner for the invite link.</p>
            )
          )}

          {mine?.status === 'PENDING' && <p className="notice">Your payment of {formatMoney(pot.contribution)} is being processed...</p>}

          {mine?.status === 'ACTIVE' && (
            <p className="notice">You are in: your {formatMoney(pot.contribution)} is in the pot.</p>
          )}

          {isOwner && pot.inviteCode && (
            <div className="stack-sm">
              <p className="label">Invite people with this private link</p>
              <InviteLink potId={pot.id} code={pot.inviteCode} />
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
      {pot.status === 'COMPLETED' && <p className="notice">Every member has won once. This pot is complete.</p>}

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
                <span className={member.won ? 'chip' : 'chip chip-muted'}>{member.won ? `won round ${member.wonRound}` : 'has not won yet'}</span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
