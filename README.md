# Cubeia Wallet

REST bookkeeping service. **Thread-safe and cluster-ready.**

## Requirements

JDK 17, 21, or 23.

## Build & run

```bash
./mvnw test
./mvnw spring-boot:run
```

## API

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

`idempotencyKey` is optional but recommended for clients that may retry on
network timeouts. Sending the same key twice returns the original transaction
without creating a duplicate.

Insufficient funds returns `422 INSUFFICIENT_FUNDS` with `currentBalance` and
`requestedDebit` in the `details` map.

## Concurrency model

This service is designed to run as a cluster of nodes against a shared
database. The correctness story:

- `Account` has a `@Version` field — JPA appends `WHERE version = ?` to every
  UPDATE.
- On version conflict, `ObjectOptimisticLockingFailureException` is thrown
  and `WalletService.transfer()` retries the operation up to 5 times with a
  50ms wait.
- The retry loop lives in a separate bean from `@Transactional executeTransfer`
  so Spring's AOP proxy starts a fresh database transaction on every attempt.
- `idempotencyKey` is enforced by a UNIQUE database index; concurrent same-key
  requests are gracefully de-duplicated.


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
