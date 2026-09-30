'use client';

import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

const QUICK_POLL_MS = 3000;
const SAFETY_POLL_MS = 30000;
const RECONNECT_MS = 5000;

/**
 * Keeps a pot page up to date. It opens the pot's WebSocket and refreshes the page whenever the server says "changed".
 * While there is no connection (before it opens, or after it drops) it falls back to refreshing every few seconds and
 * keeps trying to reconnect; with a connection it only refreshes now and then as a safety net.
 */
export function LiveUpdates({ potId }: { potId: string }) {
  const router = useRouter();

  useEffect(() => {
    let socket: WebSocket | null = null;
    let poll: ReturnType<typeof setInterval> | undefined;
    let retry: ReturnType<typeof setTimeout> | undefined;
    let stopped = false;

    const pollEvery = (ms: number) => {
      clearInterval(poll);
      poll = setInterval(() => router.refresh(), ms);
    };

    async function connect() {
      try {
        const response = await fetch(`/api/pots/${potId}/socket`, { cache: 'no-store' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const { url } = (await response.json()) as { url: string };
        if (stopped) return;
        socket = new WebSocket(url);
        socket.onopen = () => pollEvery(SAFETY_POLL_MS);
        socket.onmessage = () => router.refresh();
        socket.onerror = () => socket?.close();
        socket.onclose = () => {
          if (stopped) return;
          pollEvery(QUICK_POLL_MS);
          retry = setTimeout(connect, RECONNECT_MS);
        };
      } catch {
        if (stopped) return;
        pollEvery(QUICK_POLL_MS);
        retry = setTimeout(connect, RECONNECT_MS);
      }
    }

    pollEvery(QUICK_POLL_MS);
    void connect();
    return () => {
      stopped = true;
      clearInterval(poll);
      clearTimeout(retry);
      socket?.close();
    };
  }, [potId, router]);

  return null;
}
