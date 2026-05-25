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


# Postman collection — Cubeia Wallet

Manual-test collection for the Wallet REST API.

## Files

- `Cubeia-Wallet.postman_collection.json` — the requests.
- `Cubeia-Wallet.postman_environment.json` — a `Local` environment (`baseUrl = http://localhost:8080`).

## Import

In Postman: **Import** → drop both files. Select the **Cubeia Wallet - Local**
environment from the top-right dropdown. (The collection also ships with its own
`baseUrl`/`accountId` variables, so it works without the environment too.)

## Run order

1. **Health → Health check** — confirm the app is up.
2. **Accounts → Create account** — the response id is saved into the `accountId`
   variable automatically; every later request reuses it.
3. **Transfers → Credit / Debit** — move money; check `preBalance`/`postBalance`.
4. **Accounts → Get balance** and **Transfers → List transactions** — verify state.

The **Negative cases** folder covers 400 (validation), 404 (not found),
409 (duplicate) and 422 (insufficient funds). The *duplicate* request uses a
fixed reference: the first send returns 201, sends after that return 409.

## Notes

- Amounts are in **minor units** (cents): `500` = 5.00.
- `idempotencyKey` is auto-generated per request with `{{$guid}}`; replaying the
  same key returns the original transaction instead of creating a new one.
- **Events → Subscribe** is a Server-Sent Events stream. The Postman runner
  doesn't handle SSE well — prefer a browser or `curl -N {{baseUrl}}/api/v1/events`.

## CLI (optional)

```bash
npm install -g newman
newman run Cubeia-Wallet.postman_collection.json \
  -e Cubeia-Wallet.postman_environment.json
```
