# Cubeia Wallet

REST bookkeeping service. All four spec endpoints work — single-threaded.

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
  "description": "Deposit"
}
```

`type` is `CREDIT` (funds in) or `DEBIT` (funds out). `amount` is always
positive. Transfers inherit the account's currency — no `currency` field on
the request.

Insufficient funds returns `422 INSUFFICIENT_FUNDS` with `currentBalance` and
`requestedDebit` in `details`.

### curl

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
