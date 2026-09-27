import { identityUseCases } from './identity/index.js';
import { potUseCases } from './pot/index.js';
import { walletUseCases } from './wallet/index.js';

export * from './identity/index.js';
export * from './pot/index.js';
export * from './wallet/index.js';

/** Every use case of the platform, for the contract test and for tools. */
export const allUseCases = [...identityUseCases, ...walletUseCases, ...potUseCases] as const;

export { identityUseCases, potUseCases, walletUseCases };
