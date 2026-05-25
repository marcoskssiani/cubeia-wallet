# Cubeia Wallet

REST bookkeeping service. Work in progress.

## Requirements

JDK 17, 21, or 23.

## Build & run

```bash
./mvnw test
./mvnw spring-boot:run
```

## API (so far)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/accounts` | List all accounts |
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts/{id}` | Get an account |
| GET | `/api/v1/accounts/{id}/balance` | Get balance |

All amounts in cents. `1000 = €10.00`.

```bash
# Create
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"externalReference":"alice","currency":"EUR","initialBalance":5000}'

# Get balance
curl http://localhost:8080/api/v1/accounts/{id}/balance
```

The H2 console is at `http://localhost:8080/h2-console` (`jdbc:h2:mem:walletdb`).
