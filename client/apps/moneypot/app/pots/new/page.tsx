import { ActionForm, Field } from '@/components/ActionForm';
import { createPot } from '@/lib/actions';
import { requireUser } from '@/lib/session';

export default async function NewPotPage() {
  await requireUser('/pots/new');
  return (
    <div className="card" style={{ maxWidth: '32rem', margin: '1rem auto' }}>
      <h1 style={{ fontSize: '1.4rem', fontWeight: 800, marginBottom: '1rem' }}>Create a pot</h1>
      <ActionForm action={createPot} submitLabel="Create pot">
        <Field label="Name" name="name" placeholder="Friends monthly pot" maxLength={60} required />
        <Field label="Members" name="memberCount" type="number" min={2} max={50} defaultValue={10} hint="Everyone wins once, so this is also the number of rounds." required />
        <Field label="Each member pays every round (₹)" name="contribution" inputMode="decimal" defaultValue="5000" hint="The pot is this times the members." required />
        <Field label="Lowest bid allowed (% of the pot)" name="minBidPercent" type="number" min={1} max={100} defaultValue={60} required />
        <Field label="Bidding stays open for (minutes)" name="bidWindowMinutes" type="number" min={1} defaultValue={1440} hint="1440 minutes is one day." required />
      </ActionForm>
    </div>
  );
}
