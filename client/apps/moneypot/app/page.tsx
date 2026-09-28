import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getUser } from '@/lib/session';

export default async function Home() {
  if (await getUser()) redirect('/pots');
  return (
    <div style={{ maxWidth: '42rem', margin: '3rem auto', textAlign: 'center' }}>
      <h1 style={{ fontSize: '2.4rem', fontWeight: 800, lineHeight: 1.15 }}>Save together. Bid for the pot.</h1>
      <p className="muted" style={{ fontSize: '1.1rem', margin: '1rem 0 2rem' }}>
        A group puts the same amount in every month. Each month the members who have not won yet bid for the money, and
        the <strong>lowest bid wins</strong>. What the winner leaves behind is shared back with everyone else.
      </p>
      <div style={{ display: 'flex', gap: '0.8rem', justifyContent: 'center' }}>
        <Link href="/register" className="btn btn-primary">Create an account</Link>
        <Link href="/login" className="btn">Sign in</Link>
      </div>
      <div className="card" style={{ marginTop: '3rem', textAlign: 'left' }}>
        <strong>An example</strong>
        <p className="muted" style={{ marginTop: '0.4rem' }}>
          Ten members put in ₹5,000 each: the pot is ₹50,000. Asha bids ₹41,000 and wins it. The ₹9,000 she left is
          shared by the other nine members, ₹1,000 each. Next month Asha cannot win again, and the rest bid.
        </p>
      </div>
    </div>
  );
}
