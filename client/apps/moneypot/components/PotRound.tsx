import Link from 'next/link';
import type { PotResponse, RoundResponse } from '@moneypot/platform-sdk';
import { payContribution, settleRound } from '@/lib/actions';
import { formatMoney } from '@/lib/format';
import { Avatar, MemberTag } from './Avatar';
import { BidForm } from './BidForm';
import { Confetti } from './Confetti';
import { Countdown } from './Countdown';
import { Icon } from './icons';
import { Stepper } from './Stepper';
import { SubmitButton } from './SubmitButton';

const STEPS = [
  { key: 'COLLECTING', label: 'Pay in' },
  { key: 'BIDDING', label: 'Bid' },
  { key: 'SETTLING', label: 'Payout' },
];

const TRANSFER = {
  REQUESTED: { label: 'Paying...', chip: 'chip chip-warn' },
  DONE: { label: 'Paid', chip: 'chip' },
  REJECTED: { label: 'Not paid', chip: 'chip chip-bad' },
  VOID: { label: '', chip: 'chip chip-muted' },
} as const;

/** The running round of a pot: what has been paid, the bids, and what the member can do now. */
export function PotRound({ pot, round, me, isOwner }: { pot: PotResponse; round: RoundResponse; me: string; isOwner: boolean }) {
  const mine = pot.members.find((m) => m.userId === me);
  const contributions = round.transfers.filter((t) => t.kind === 'CONTRIBUTION');
  const myContribution = contributions.find((t) => t.userId === me);
  const lowest = round.bids[0];
  const myBid = round.bids.find((b) => b.userId === me);
  const order = (userId: string) => pot.members.find((m) => m.userId === userId)?.joinOrder ?? 0;

  return (
    <div className="card stack">
      <div className="between">
        <h2 className="section-title" style={{ fontSize: '1.3rem' }}>Round {round.number} of {pot.memberCount}</h2>
        <span className="live">Live</span>
      </div>
      <Stepper steps={STEPS} current={round.status} />

      {round.status === 'COLLECTING' && (
        <>
          <p className="muted">Everyone pays {formatMoney(pot.contribution)} into the pot. Bidding opens when all {pot.memberCount} have paid.</p>
          <div>
            {contributions.map((t) => (
              <div className="row" key={t.userId}>
                <MemberTag userId={t.userId} members={pot.members} me={me} />
                <span className={TRANSFER[t.status].chip}>{TRANSFER[t.status].label}</span>
              </div>
            ))}
          </div>
          {myContribution?.status === 'REJECTED' && (
            <div className="notice notice-error stack-sm">
              <span>Your payment did not go through{myContribution.reason ? `: ${myContribution.reason}` : ''}. Add money to your wallet, then pay again.</span>
              <div className="cluster">
                <Link href="/wallet" className="btn">Open wallet</Link>
                <form action={payContribution}>
                  <input type="hidden" name="potId" value={pot.id} />
                  <SubmitButton>Pay {formatMoney(pot.contribution)} again</SubmitButton>
                </form>
              </div>
            </div>
          )}
        </>
      )}

      {round.status === 'BIDDING' && (
        <>
          <div className="between">
            <div>
              <p className="eyebrow">{lowest ? 'Lowest bid so far' : 'No bids yet'}</p>
              <p className="big-number">{formatMoney(lowest ? lowest.amount : pot.potAmount)}</p>
              {!lowest && <p className="muted small">If nobody bids, the next member in order gets the whole pot.</p>}
            </div>
            {round.biddingEndsAt && (
              <div className="card card-flat" style={{ textAlign: 'center' }}>
                <p className="eyebrow cluster" style={{ justifyContent: 'center', gap: '0.3rem' }}><Icon name="clock" size={14} /> Bidding closes in</p>
                <p style={{ fontSize: '1.3rem', fontWeight: 800 }}><Countdown endsAt={round.biddingEndsAt} suffix="" /></p>
              </div>
            )}
          </div>

          {mine?.won ? (
            <p className="notice">You already won round {mine.wonRound}, so you sit this one out. You still get a share of what the winner leaves.</p>
          ) : (
            <BidForm potId={pot.id} potAmount={pot.potAmount} minBid={pot.minBid} memberCount={pot.memberCount} lowest={lowest?.amount ?? null} mine={myBid?.amount ?? null} />
          )}

          {round.bids.length > 0 && (
            <div className="stack-sm">
              <p className="eyebrow">Bids, lowest first</p>
              {round.bids.map((bid, index) => (
                <div key={bid.userId} style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', alignItems: 'center', gap: '0.8rem' }}>
                  <Avatar order={order(bid.userId)} me={bid.userId === me} size={32} />
                  <div className="stack-sm" style={{ gap: '0.25rem' }}>
                    <span style={{ fontWeight: 650 }}>
                      Member {order(bid.userId)}{bid.userId === me && <span className="muted"> (you)</span>}
                      {index === 0 && <span className="chip" style={{ marginLeft: '0.5rem' }}>leading</span>}
                    </span>
                    <div className="track"><div className={index === 0 ? 'track-fill track-fill-gold' : 'track-fill'} style={{ width: `${Math.min(100, (bid.amount / pot.potAmount) * 100)}%` }} /></div>
                  </div>
                  <strong>{formatMoney(bid.amount)}</strong>
                </div>
              ))}
            </div>
          )}

          {isOwner && (
            <form action={settleRound}>
              <input type="hidden" name="potId" value={pot.id} />
              <SubmitButton primary={false}>End bidding now</SubmitButton>
            </form>
          )}
        </>
      )}

      {round.status === 'SETTLING' && round.winnerId && round.payout !== null && (
        <div className="hero-card">
          <Confetti />
          <div className="cluster" style={{ gap: '0.8rem' }}>
            <span className="icon-bubble icon-bubble-gold"><Icon name="trophy" size={22} /></span>
            <div>
              <p className="muted-on">Winner of round {round.number}</p>
              <p style={{ fontSize: '1.4rem', fontWeight: 800 }}>Member {order(round.winnerId)}{round.winnerId === me && ' (you)'} wins {formatMoney(round.payout)}</p>
            </div>
          </div>
          {pot.potAmount > round.payout && (
            <p className="muted-on" style={{ marginTop: '0.8rem' }}>
              The other {formatMoney(pot.potAmount - round.payout)} is being shared among the other {pot.memberCount - 1} members. The next round
              starts when everything is paid.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
