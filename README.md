# MoneyPot

A savings-pot (kameti) platform. Friends form a **pot**; every month (a **round**) each member pays in, the members who
have not won yet **bid** for the money, and the **lowest bid wins**. What the winner leaves (pot minus the winning bid)
goes back to the other members as a dividend. A member who has won cannot win again, so a pot of N members runs N rounds.
The last member left is paid the whole pot.

Example: 10 members, Rs 5,000 each, pot Rs 50,000. Asha bids Rs 41,000 and wins it. The Rs 9,000 she left is shared by the
other nine: Rs 1,000 each.

## How it is built

The same structure as the shop: hexagonal services, the transactional outbox and inbox, one class per use case.

- `server/platform` the Maven backend (`com.moneypot`)
  - `module/*` shared building blocks: `shared`, `web`, `security`, `messaging` (outbox, inbox, event contracts)
  - `infra` named clients (RabbitMQ) and the adapters that plug the outbox and inbox into them
  - `service/identity` (9086) sign-up, login, JWT
  - `service/wallet` (9081) one account per member and per pot, an append-only ledger, atomic transfers
  - `service/pot` (9082) pots, members, rounds, bids; settles rounds; tells watching pages about changes (WebSocket)
  - `bootstrap/local` Postgres and RabbitMQ for development, and `e2e.py`
- `client`
  - `packages/sdk-core` the engine behind the typed SDK, `packages/config` shared TypeScript settings
  - `service-sdk/platform` `@moneypot/platform-sdk`, one use case per endpoint
  - `apps/moneypot` `@moneypot/web`, the Next.js app (port 3100)

### How a round moves

The pot and the wallet only talk through events (RabbitMQ, through the outbox and inbox):

1. Members pay in: the pot sends `Pot.TransferRequested` for every member; the wallet moves the money and answers
   `Wallet.TransferCompleted` (or `TransferRejected`, for example when the balance is too low).
2. When everyone has paid, bidding opens. The lowest bid wins when the time is over (or when the owner ends it).
3. The pot asks the wallet to pay the winner the bid, and to give the others their dividend. When all are paid the
   round is settled and the next one starts. After the last round the pot is complete.

Money is always a whole number of **paise** in the services; the pages show rupees.

## Run it locally

You need Java 21, Maven, Node 20+, pnpm and Docker.

```
server/platform/bootstrap/local/local.ps1 up              # Postgres (5433) and RabbitMQ (5673)
server/platform/bootstrap/local/dev-service.ps1 identity  # one window each
server/platform/bootstrap/local/dev-service.ps1 wallet
server/platform/bootstrap/local/dev-service.ps1 pot
pnpm install
pnpm --filter @moneypot/web dev                           # http://localhost:3100
```

The ports differ from the shop's, so both projects can run on one machine. To use a hosted RabbitMQ, put `RABBITMQ_HOST`,
`RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` and `RABBITMQ_SSL` in a git-ignored `.env`
at the repository root; the exchanges and queues are named `moneypot...`, so they do not clash with another project.

With the services running, `python server/platform/bootstrap/local/e2e.py` plays a whole three-member pot through them.

## Tests

```
cd server/platform && mvn test      # services, modules and infra
pnpm test                           # the SDK
pnpm typecheck
```
