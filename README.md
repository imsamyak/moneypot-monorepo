# MoneyPot

A savings-pot (kameti) platform. Friends form a **pot**; every month (a **round**) each member pays in, members **bid**
for the money, and the **lowest bid wins**. The part nobody took (pot minus the winning bid) goes back to the other
members' wallets as a dividend. A member who has won cannot win again.

## Layout

- `server/platform` the Maven backend (`com.moneypot`)
  - `module/*` shared building blocks: `shared`, `web`, `security`, `messaging` (outbox, inbox, event contracts)
  - `infra` named clients (RabbitMQ) and the adapters that plug the outbox and inbox into them
  - `service/identity` sign-up, login, JWT
  - `service/wallet` accounts and the ledger
  - `service/pot` pots, members, rounds, bids
  - `bootstrap/local` Postgres and RabbitMQ for development
- `client` SDKs and the Next.js app

## Run locally

```
server/platform/bootstrap/local/local.ps1 up
server/platform/bootstrap/local/dev-service.ps1 identity
server/platform/bootstrap/local/dev-service.ps1 wallet
server/platform/bootstrap/local/dev-service.ps1 pot
```
