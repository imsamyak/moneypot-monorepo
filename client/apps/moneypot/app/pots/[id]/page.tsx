import { SdkHttpError, type PotResponse } from '@moneypot/platform-sdk';
import { notFound } from 'next/navigation';
import { LiveUpdates } from '@/components/LiveUpdates';
import { PotHistory } from '@/components/PotHistory';
import { PotRound } from '@/components/PotRound';
import { SubmitButton } from '@/components/SubmitButton';
import { joinPot, startPot } from '@/lib/actions';
import { formatMoney, memberLabel } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const STATUS_LABEL = { OPEN: 'Open to join', ACTIVE: 'Running', COMPLETED: 'Completed' } as const;

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
  const full = pot.members.length >= pot.memberCount;

  return (
    <div style={{ display: 'grid', gap: '1.2rem' }}>
      {mine && pot.status !== 'COMPLETED' && <LiveUpdates potId={pot.id} />}
      {error && <p className="notice notice-error" role="alert">{error}</p>}

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800 }}>{pot.name}</h1>
            <p className="muted">
              {formatMoney(pot.contribution)} each from {pot.memberCount} members. The lowest bid wins, and what is left is shared.
            </p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span className={pot.status === 'OPEN' ? 'badge badge-warn' : 'badge'}>{STATUS_LABEL[pot.status]}</span>
            <p style={{ fontSize: '1.8rem', fontWeight: 800 }}>{formatMoney(pot.potAmount)}</p>
          </div>
        </div>
      </div>

      {pot.status === 'OPEN' && (
        <div className="card" style={{ display: 'grid', gap: '0.8rem' }}>
          <p>
            <strong>{pot.members.length} of {pot.memberCount}</strong> members have joined.
            {!full && ' Share this page with the others so they can join.'}
          </p>
          {!mine && !full && (
            <form action={joinPot}>
              <input type="hidden" name="potId" value={pot.id} />
              <SubmitButton>Join this pot</SubmitButton>
            </form>
          )}
          {isOwner && (
            <form action={startPot} style={{ display: 'grid', gap: '0.4rem' }}>
              <input type="hidden" name="potId" value={pot.id} />
              <div><SubmitButton>Start the pot</SubmitButton></div>
              {!full && <span className="muted" style={{ fontSize: '0.85rem' }}>You can start once all {pot.memberCount} members have joined.</span>}
            </form>
          )}
        </div>
      )}

      {pot.status === 'COMPLETED' && (
        <p className="notice">Every member has won once. This pot is complete.</p>
      )}

      {pot.round && mine && pot.status === 'ACTIVE' && <PotRound pot={pot} round={pot.round} me={user.id} isOwner={isOwner} />}
      {pot.round && !mine && pot.status === 'ACTIVE' && (
        <p className="notice">Round {pot.round.number} is running. Only members can see the details.</p>
      )}

      <PotHistory pot={pot} me={user.id} />

      <div className="card">
        <h2 style={{ fontWeight: 800, marginBottom: '0.4rem' }}>Members</h2>
        {pot.members.map((member) => (
          <div className="row" key={member.userId}>
            <span>
              {memberLabel(member.userId, pot.members, user.id)}
              {member.userId === pot.ownerId && <span className="badge badge-warn" style={{ marginLeft: '0.5rem' }}>owner</span>}
            </span>
            <span className={member.won ? 'badge' : 'muted'}>{member.won ? `won round ${member.wonRound}` : 'has not won yet'}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
