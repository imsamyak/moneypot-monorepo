import { describe, expect, it } from 'vitest';
import { allUseCases } from '../src/index.js';

/** The endpoints the services expose, as `METHOD path` with the sign-in requirement. Keep it in step with the controllers. */
const backend: Record<string, string> = {
  Register: 'POST /api/auth/register',
  Login: 'POST /api/auth/login',
  Logout: 'POST /api/auth/logout',
  GetCurrentUser: 'GET /api/auth/me',
  ForgotPassword: 'POST /api/auth/forgot-password',
  ResetPassword: 'POST /api/auth/reset-password',
  OpenWallet: 'POST /api/wallet',
  GetWallet: 'GET /api/wallet',
  Deposit: 'POST /api/wallet/deposit',
  GetLedger: 'GET /api/wallet/ledger',
  CreatePot: 'POST /api/pots',
  ListPots: 'GET /api/pots',
  GetPot: 'GET /api/pots/{potId}',
  JoinPot: 'POST /api/pots/{potId}/join',
  LeavePot: 'POST /api/pots/{potId}/leave',
  PayContribution: 'POST /api/pots/{potId}/contribution',
  PlaceBid: 'POST /api/pots/{potId}/bids',
  SettleRound: 'POST /api/pots/{potId}/settle',
  IssueSocketTicket: 'POST /api/pots/{potId}/socket-ticket',
};

describe('the SDK use cases', () => {
  it('match the endpoints of the services, no more and no fewer', () => {
    const sdk = Object.fromEntries(allUseCases.map((u) => [u.name, `${u.method} ${u.path}`]));

    expect(sdk).toEqual(backend);
  });

  it('send the services their own base URL name', () => {
    const services = new Set(allUseCases.map((u) => u.service));

    expect([...services].sort()).toEqual(['identity', 'pot', 'wallet']);
  });

  it('ask for a token on everything except signing up, signing in and out, and password reset', () => {
    const open = new Set(['Register', 'Login', 'Logout', 'ForgotPassword', 'ResetPassword']);

    for (const useCase of allUseCases) {
      expect(useCase.authRequired, useCase.name).toBe(!open.has(useCase.name));
    }
  });
});
