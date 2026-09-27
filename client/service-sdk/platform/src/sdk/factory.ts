import { SdkBuilder, type SdkOptions } from '@moneypot/sdk-core';
import { type BaseUrls, resolveBaseUrls } from '../config/env.js';
import { identityUseCases, potUseCases, walletUseCases } from '../usecases/index.js';

/** The engine's options; `baseUrls` only overrides the addresses read from the environment. */
export type MoneyPotSdkOptions<Ctx> = Omit<SdkOptions<Ctx>, 'baseUrls'> & { readonly baseUrls?: Partial<BaseUrls> };

/** A builder with every MoneyPot use case in its gateway; chain more `.addGateway(...)` or call `.build()`. */
export function moneyPotSdkBuilder<Ctx = undefined>(options: MoneyPotSdkOptions<Ctx>) {
  return SdkBuilder.create<Ctx>({ ...options, baseUrls: resolveBaseUrls(undefined, options.baseUrls) })
    .addGateway('auth', identityUseCases)
    .addGateway('wallet', walletUseCases)
    .addGateway('pots', potUseCases);
}

/** The SDK with every MoneyPot use case: `const { auth, wallet, pots } = sdk.with()`. */
export function createMoneyPotSdk<Ctx = undefined>(options: MoneyPotSdkOptions<Ctx>) {
  return moneyPotSdkBuilder(options).build();
}

export type MoneyPotSdk<Ctx = undefined> = ReturnType<typeof createMoneyPotSdk<Ctx>>;

/** The gateways `sdk.with(ctx)` returns. */
export type MoneyPotGateways<Ctx = undefined> = ReturnType<MoneyPotSdk<Ctx>['with']>;
