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
