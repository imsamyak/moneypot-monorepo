# MoneyPot web

The app for MoneyPot: sign up, top up a wallet, create or join a savings pot, bid for the pot each round, and watch the
money move.

```
pnpm --filter @moneypot/web dev     # http://localhost:3100
```

Start the services first (see the repository README). The pages are server components; every action (sign in, top up,
join, bid, ...) is a server action that calls the services through `@moneypot/platform-sdk`. The access token never
reaches the browser: it lives in an httpOnly cookie.

- `app/` the pages: `/` (home), `/login`, `/register`, `/wallet`, `/pots`, `/pots/new`, `/pots/[id]`
- `components/` the few client components (forms, the live updates while a pot is running)
- `lib/` the SDK instance, the signed-in user, the server actions, and money formatting (money is paise in the services)
