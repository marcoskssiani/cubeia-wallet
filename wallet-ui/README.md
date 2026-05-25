# Wallet Dashboard UI

Real-time monitoring dashboard for the Cubeia Wallet API.

## Stack

- **Vite 5** + **React 18** + **TypeScript**
- **Tailwind CSS** — dark slate theme
- **Lucide React** — icons
- **Server-Sent Events** — live transaction feed (no WebSocket needed)

## Running

**Requires Node 18+.** If you use nvm: `nvm use 18`

```bash
cd wallet_test/wallet-ui
npm install
npm run dev          # starts on http://localhost:5173
```

The Vite dev server proxies `/api` → `http://localhost:8080`, so make sure the Spring Boot backend is running first:

```bash
# In another terminal, from wallet_test/
mvn spring-boot:run
```

## Features

| Area | Detail |
|---|---|
| **Header stats** | Total accounts, total balance, transactions in last 5 min, live SSE connections |
| **Account list** | Left panel — all accounts, live-polling every 3 s |
| **Account detail** | Center panel — balance (live), paginated transaction history |
| **Live feed** | Right panel — real-time SSE stream, last 100 events, color-coded CREDIT/DEBIT |
| **Transfer modal** | Credit or debit any account from any panel |
| **Create account** | Modal form — externalReference, currency, optional initial balance |
| **Demo data** | 5 seed accounts (alice/bob/charlie/diana/eve) with initial transactions, created on backend startup |

## Architecture notes

- `usePolling<T>(fetcher, intervalMs)` — generic polling hook used for accounts list and account detail
- `useActivityFeed()` — opens a single `EventSource` to `/api/v1/events` and maintains the last 100 events in state
- `walletApi.ts` — thin fetch wrapper; throws `Error` on non-2xx responses
- Vite proxy eliminates CORS entirely — the browser sees one origin
