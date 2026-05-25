# Cubeia Wallet - by Marcos Cassiani

REST bookkeeping service. **Thread-safe and cluster-ready.** Ships with a
Vite + React demo UI and an SSE live-activity feed.

## Requirements

- JDK 17, 21, or 23 (Mockito / ByteBuddy are pinned in `pom.xml` for forward
  compatibility; JDK 24+ is untested).
- Node 18+ (for the optional `wallet-ui/`).

## Build & run — backend

```bash
./mvnw test
./mvnw spring-boot:run
```

Seeds 5 demo accounts and 12 transactions on startup. The H2 console is at
`http://localhost:8080/h2-console` (`jdbc:h2:mem:walletdb`, user `sa`, no
password).

## Build & run — frontend

```bash
cd wallet-ui
npm install
npm run dev
# open http://localhost:5173
```

## Spec-required API

All amounts in cents. `1000 = €10.00`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/accounts` | List accounts |
| POST | `/api/v1/accounts` | Create account |
| GET | `/api/v1/accounts/{id}` | Get account |
| GET | `/api/v1/accounts/{id}/balance` | Get balance |
| POST | `/api/v1/accounts/{id}/transfers` | Credit or debit |
| GET | `/api/v1/accounts/{id}/transactions` | List transactions (paginated) |

### Transfer

```http
POST /api/v1/accounts/{id}/transfers
{
  "amount": 1000,
  "type": "CREDIT",
  "description": "Deposit",
  "idempotencyKey": "optional-uuid-for-safe-retries"
}
```

`type` is `CREDIT` (funds in) or `DEBIT` (funds out). `amount` is always
positive. Transfers inherit the account's currency.

`idempotencyKey` is optional but recommended — sending the same key twice
returns the original transaction without creating a duplicate. The bundled
frontend generates one per submit so a double-clicked button never
double-charges an account.

Insufficient funds returns `422 INSUFFICIENT_FUNDS` with `currentBalance` and
`requestedDebit` in `details`.

## Bonus endpoints (beyond the spec — used by the demo UI)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/accounts/stats` | Aggregate counts for the dashboard |
| GET | `/api/v1/events` | SSE stream of `transaction` / `heartbeat` events |
| GET | `/api/v1/events/connections` | Active SSE connection count |

These are not part of the original interview spec; they exist to make the
demo UI useful. They can be removed by deleting `sse/`, `event/`,
`StatsResponse.java`, and the `/stats` controller mapping with no impact on
the core wallet behaviour.

## curl

```bash
BASE=http://localhost:8080/api/v1

ACCOUNT_ID=$(curl -sX POST $BASE/accounts \
  -H 'Content-Type: application/json' \
  -d '{"externalReference":"alice","currency":"EUR"}' | jq -r .id)

curl -sX POST $BASE/accounts/$ACCOUNT_ID/transfers \
  -H 'Content-Type: application/json' \
  -d '{"amount":5000,"type":"CREDIT","description":"Deposit"}' | jq

curl -s $BASE/accounts/$ACCOUNT_ID/balance | jq
curl -s "$BASE/accounts/$ACCOUNT_ID/transactions?page=0&size=10" | jq
```

## Known limitations

1. **In-memory H2.** Data is lost on restart. Swap `application.properties`
   to PostgreSQL — the optimistic locking and idempotency are
   database-agnostic.
2. **2-decimal currencies only.** JPY (0 decimals) and KWD (3 decimals)
   would need a currency → decimal-places lookup.
3. **No auth.** Production would require OAuth2/JWT with per-account
   authorization.

## Tests

```bash
./mvnw test
```

| Class | Scope |
|---|---|
| `WalletApplicationTests` | Spring context loads |
| `WalletServiceTest` | Unit + idempotency happy path |
| `ConcurrencyTest` | Concurrent debits / credits / idempotent same-key |


## Postman

A ready-to-import collection and local environment live in the `postman/`
directory for manual testing. See [postman/README.md](postman/README.md) for
import and run-order instructions.
