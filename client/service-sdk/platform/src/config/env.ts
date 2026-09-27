/** The services the platform use cases are sent to; each has its own base URL. */
export type ServiceName = 'identity' | 'wallet' | 'pot';

export type BaseUrls = Record<ServiceName, string>;

type Env = Record<string, string | undefined>;

/** Per-service variable; it wins over `MONEYPOT_API_URL`. */
export const SERVICE_URL_ENV: Readonly<Record<ServiceName, string>> = {
  identity: 'MONEYPOT_IDENTITY_URL',
  wallet: 'MONEYPOT_WALLET_URL',
  pot: 'MONEYPOT_POT_URL',
};

/** One base URL for every service, for when they sit behind a gateway. */
export const API_URL_ENV = 'MONEYPOT_API_URL';

/** The `server.port` defaults of the services' `application.yml`. */
const LOCAL_DEFAULTS: Readonly<BaseUrls> = {
  identity: 'http://localhost:9086',
  wallet: 'http://localhost:9081',
  pot: 'http://localhost:9082',
};

function processEnv(): Env {
  return (globalThis as { process?: { env?: Env } }).process?.env ?? {};
}

const trimSlash = (url: string): string => url.replace(/\/+$/, '');

/** Base URL of every service: the per-service variable, else `MONEYPOT_API_URL`, else the local default. */
export function resolveBaseUrls(env: Env = processEnv(), overrides: Partial<BaseUrls> = {}): BaseUrls {
  const shared = env[API_URL_ENV];
  const urls = {} as BaseUrls;
  for (const service of Object.keys(LOCAL_DEFAULTS) as ServiceName[]) {
    urls[service] = trimSlash(overrides[service] ?? env[SERVICE_URL_ENV[service]] ?? shared ?? LOCAL_DEFAULTS[service]);
  }
  return urls;
}
