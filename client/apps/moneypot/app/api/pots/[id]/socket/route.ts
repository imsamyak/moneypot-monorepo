import { resolveBaseUrls } from '@moneypot/platform-sdk';
import { errorMessage } from '@/lib/errors';
import { api } from '@/lib/sdk';
import { getUser } from '@/lib/session';

/**
 * The address the browser opens to watch a pot live. The access token stays in the httpOnly cookie, so the browser cannot
 * send it on a WebSocket: this route (which can read the cookie) asks the pot service for a one-time ticket, and the
 * browser opens the connection with that.
 */
export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  if (!(await getUser())) return Response.json({ error: 'Sign in to continue.' }, { status: 401 });
  const { id } = await params;
  try {
    const { ticket } = await api().pots.issueSocketTicket({ potId: id });
    const base = (process.env.MONEYPOT_POT_WS_URL ?? resolveBaseUrls().pot.replace(/^http/, 'ws')).replace(/\/+$/, '');
    return Response.json({ url: `${base}/api/ws/pots?ticket=${encodeURIComponent(ticket)}` }, { headers: { 'Cache-Control': 'no-store' } });
  } catch (error) {
    return Response.json({ error: errorMessage(error) }, { status: 502 });
  }
}
