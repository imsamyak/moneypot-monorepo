# @moneypot/platform-sdk

The typed client for the MoneyPot services: sign-up and login (identity), the wallet, and the pots. It is built on
`@moneypot/sdk-core`; every endpoint is one use case, grouped in three gateways.

```ts
const sdk = createMoneyPotSdk({ tokenStore });
const { auth, wallet, pots } = sdk.with();

await auth.login({ email, password });
await wallet.deposit({ amount: 500_000 }); // paise: Rs 5,000
const pot = await pots.createPot({ name: 'Friends', memberCount: 10, contribution: 500_000 });
await pots.placeBid({ potId: pot.id, amount: 4_100_000 });
```

Money is always an integer number of **paise** (1/100 of a rupee).

Where each service is comes from `MONEYPOT_API_URL` (one address for all) or `MONEYPOT_IDENTITY_URL`,
`MONEYPOT_WALLET_URL`, `MONEYPOT_POT_URL`; otherwise the local ports (9086, 9081, 9082).

A pot can also be watched live: `pots.issueSocketTicket` gives a one-time ticket for `ws://<pot service>/api/ws/pots?ticket=...`,
which sends `changed` whenever the pot does.
