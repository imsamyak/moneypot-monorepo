import { memberHue } from '@/lib/format';

/** A member as a coloured circle with the number they joined at. */
export function Avatar({ order, me = false, size = 36 }: { order: number; me?: boolean; size?: number }) {
  return (
    <span
      className="avatar"
      title={`Member ${order}`}
      style={{
        width: size,
        height: size,
        fontSize: size * 0.4,
        background: `hsl(${memberHue(order)} 52% 40%)`,
        outline: me ? '2px solid var(--brand)' : undefined,
        outlineOffset: 2,
      }}
    >
      {order}
    </span>
  );
}

/** A member's avatar and name ("Member 2 (you)"). */
export function MemberTag({
  userId,
  members,
  me,
  size = 32,
}: {
  userId: string;
  members: { userId: string; joinOrder: number }[];
  me: string;
  size?: number;
}) {
  const member = members.find((m) => m.userId === userId);
  return (
    <span className="member">
      <Avatar order={member?.joinOrder ?? 0} me={userId === me} size={size} />
      <span>
        {member ? `Member ${member.joinOrder}` : 'Someone'}
        {userId === me && <span className="muted"> (you)</span>}
      </span>
    </span>
  );
}
