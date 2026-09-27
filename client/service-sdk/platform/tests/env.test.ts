import { describe, expect, it } from 'vitest';
import { resolveBaseUrls } from '../src/index.js';

describe('resolveBaseUrls', () => {
  it('uses the local ports of the services by default', () => {
    expect(resolveBaseUrls({})).toEqual({
      identity: 'http://localhost:9086',
      wallet: 'http://localhost:9081',
      pot: 'http://localhost:9082',
    });
  });

  it('lets one address cover every service, and a per-service address win over it', () => {
    const urls = resolveBaseUrls({ MONEYPOT_API_URL: 'https://api.example.com/', MONEYPOT_POT_URL: 'http://pot:9082' });

    expect(urls.identity).toBe('https://api.example.com');
    expect(urls.wallet).toBe('https://api.example.com');
    expect(urls.pot).toBe('http://pot:9082');
  });
});
