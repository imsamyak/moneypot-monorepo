# MoneyPot

A savings-pot platform, by invitation only.

1. Someone **creates a pot**: how much each member pays, how long each round of bidding stays open, and **when bidding
   starts**. There is no limit on members. They get a private **invite code**.
2. People **join with just that code and pay their contribution when they join**. Until bidding starts they can **leave and get
   it all back**.
3. At the start time **joining closes** (nobody can join or leave from then on) and whoever has paid is the group.
   Bidding opens straight away. If fewer than two members paid, the pot is cancelled and everyone is refunded.
4. Each round the members who have not collected yet **bid** the least they will accept, and the **lowest bid collects the
   pot**. The collector gets their bid, and what is left (pot minus bid) goes back to the other members as a dividend. A member
   who has collected cannot collect again, so a pot of N members runs N rounds, and the last member left is paid the whole pot.

Example: 10 members paid Rs 5,000 each, so the pot is Rs 50,000. Asha bids Rs 41,000 and collects it. The Rs 9,000 she left is
shared by the other nine: Rs 1,000 each.

## How it is built

The same structure as the shop: hexagonal services, the transactional outbox and inbox, one class per use case.

- `server/platform` the Maven backend (`com.moneypot`)
  - `module/*` shared building blocks: `shared`, `web`, `security`, `messaging` (outbox, inbox, event contracts)
  - `infra` named clients (RabbitMQ) and the adapters that plug the outbox and inbox into them
  - `service/identity` (9086) sign-up, login, JWT
  - `service/wallet` (9081) one account per member and per pot, an append-only ledger, atomic transfers
  - `service/pot` (9082) pots, members, rounds, bids; starts pots at their start time and settles rounds; tells watching pages about changes (WebSocket)
  - `bootstrap/local` Postgres and RabbitMQ for development, and `e2e.py`
- `client`
  - `packages/sdk-core` the engine behind the typed SDK, `packages/config` shared TypeScript settings
  - `service-sdk/platform` `@moneypot/platform-sdk`, one use case per endpoint
  - `apps/moneypot` `@moneypot/web`, the Next.js app (port 3100)

### How the money moves

The pot and the wallet only talk through events (RabbitMQ, through the outbox and inbox):

1. When someone joins, the pot sends `Pot.TransferRequested` (a contribution); the wallet moves the money and answers
   `Wallet.TransferCompleted`, or `TransferRejected` when the balance is too low (the member is then not in).
2. Leaving, or a pot that is cancelled, sends a `REFUND` the same way.
3. Later rounds: when every contribution is paid bidding opens; when bidding ends the pot asks the wallet to pay the collector
   the bid (`PAYOUT`) and give the others their share (`DIVIDEND`). When all are paid the round is settled and the next one
   starts.

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
