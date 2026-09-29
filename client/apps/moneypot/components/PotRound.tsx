import Link from 'next/link';
import type { PotResponse, RoundResponse } from '@moneypot/platform-sdk';
import { payContribution, placeBid, settleRound } from '@/lib/actions';
import { formatMoney, memberLabel } from '@/lib/format';
import { Countdown } from './Countdown';
import { SubmitButton } from './SubmitButton';

const STEPS = [
  { status: 'COLLECTING', label: 'Pay in' },
  { status: 'BIDDING', label: 'Bid' },
  { status: 'SETTLING', label: 'Payout' },
] as const;

const TRANSFER_LABEL = { REQUESTED: 'Paying...', DONE: 'Paid', REJECTED: 'Not paid' } as const;

/** The running round of a pot: what has been paid, the bids, and what the member can do now. */
export function PotRound({ pot, round, me, isOwner }: { pot: PotResponse; round: RoundResponse; me: string; isOwner: boolean }) {
  const mine = pot.members.find((m) => m.userId === me);
  const who = (userId: string) => memberLabel(userId, pot.members, me);
  const contributions = round.transfers.filter((t) => t.kind === 'CONTRIBUTION');
  const myContribution = contributions.find((t) => t.userId === me);
  const lowest = round.bids[0];
  const myBid = round.bids.find((b) => b.userId === me);

  return (
    <div className="card" style={{ display: 'grid', gap: '1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <h2 style={{ fontWeight: 800, fontSize: '1.2rem' }}>
          Round {round.number} of {pot.memberCount}
        </h2>
        <div style={{ display: 'flex', gap: '0.4rem' }}>
          {STEPS.map((step) => (
            <span key={step.status} className={step.status === round.status ? 'badge' : 'badge badge-warn'} style={{ opacity: step.status === round.status ? 1 : 0.5 }}>
              {step.label}
            </span>
          ))}
        </div>
      </div>

      {round.status === 'COLLECTING' && (
        <>
          <p className="muted">
            Everyone pays {formatMoney(pot.contribution)} into the pot. Bidding opens when all {pot.memberCount} have paid.
          </p>
          <div>
            {contributions.map((t) => (
              <div className="row" key={t.userId}>
                <span>{who(t.userId)}</span>
                <span className={t.status === 'DONE' ? 'badge' : t.status === 'REJECTED' ? 'badge badge-bad' : 'badge badge-warn'}>
                  {TRANSFER_LABEL[t.status]}
                </span>
              </div>
            ))}
          </div>
          {myContribution?.status === 'REJECTED' && (
            <div className="notice notice-error" style={{ display: 'grid', gap: '0.6rem' }}>
              <span>
                Your payment did not go through{myContribution.reason ? `: ${myContribution.reason}` : ''}. Add money to your wallet,
                then pay again.
              </span>
              <div style={{ display: 'flex', gap: '0.6rem' }}>
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
          <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <p className="muted">{lowest ? 'Lowest bid so far' : 'No bids yet'}</p>
              <p style={{ fontSize: '1.8rem', fontWeight: 800 }}>{lowest ? formatMoney(lowest.amount) : formatMoney(pot.potAmount)}</p>
              {!lowest && <p className="muted" style={{ fontSize: '0.85rem' }}>If nobody bids, the next member in order gets the whole pot.</p>}
            </div>
            {round.biddingEndsAt && (
              <div style={{ textAlign: 'right' }}>
                <p className="muted">Bidding closes</p>
                <p style={{ fontWeight: 700 }}><Countdown endsAt={round.biddingEndsAt} /></p>
              </div>
            )}
          </div>

          {mine?.won ? (
            <p className="notice">You already won round {mine.wonRound}, so you sit this one out. You still get a share of what the winner leaves.</p>
          ) : (
            <form action={placeBid} style={{ display: 'grid', gap: '0.6rem' }}>
              <input type="hidden" name="potId" value={pot.id} />
              <div>
                <label className="label" htmlFor="amount">{myBid ? 'Lower your bid (₹)' : 'Your bid (₹)'}</label>
                <input
                  className="input"
                  id="amount"
                  name="amount"
                  inputMode="decimal"
                  placeholder={String(Math.round((myBid?.amount ?? pot.potAmount) / 100) - 1)}
                  required
                />
                <p className="muted" style={{ fontSize: '0.8rem', marginTop: '0.25rem' }}>
                  What you will accept for the pot. Between {formatMoney(pot.minBid)} and {formatMoney(pot.potAmount)}.
                  The lowest bid wins{myBid ? `, and you can only go lower than your bid of ${formatMoney(myBid.amount)}` : ''}.
                </p>
              </div>
              <div><SubmitButton>{myBid ? 'Lower my bid' : 'Place bid'}</SubmitButton></div>
            </form>
          )}

          {round.bids.length > 0 && (
            <div>
              <p className="label">Bids, lowest first</p>
              {round.bids.map((bid, index) => (
                <div className="row" key={bid.userId}>
                  <span>{who(bid.userId)}{index === 0 && <span className="badge" style={{ marginLeft: '0.5rem' }}>leading</span>}</span>
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
        <>
          <p>
            <strong>{who(round.winnerId)}</strong> wins this round and gets <strong>{formatMoney(round.payout)}</strong>.
          </p>
          {pot.potAmount > round.payout && (
            <p className="muted">
              The other {formatMoney(pot.potAmount - round.payout)} is being shared among the other {pot.memberCount - 1} members.
            </p>
          )}
          <p className="muted">Paying out now. The next round starts when everything is paid.</p>
        </>
      )}
    </div>
  );
}
