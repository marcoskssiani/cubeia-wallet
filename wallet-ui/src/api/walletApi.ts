import type { Account, Transaction, TransactionList, Stats } from '../types'

const BASE = '/api/v1'

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.text()
    throw new Error(`${res.status} ${res.statusText}: ${body}`)
  }
  return res.json() as Promise<T>
}

export const walletApi = {
  listAccounts(): Promise<Account[]> {
    return fetch(`${BASE}/accounts`).then(r => json<Account[]>(r))
  },

  getAccount(id: string): Promise<Account> {
    return fetch(`${BASE}/accounts/${id}`).then(r => json<Account>(r))
  },

  getStats(): Promise<Stats> {
    return fetch(`${BASE}/accounts/stats`).then(r => json<Stats>(r))
  },

  listTransactions(accountId: string, page = 0, size = 20): Promise<TransactionList> {
    return fetch(`${BASE}/accounts/${accountId}/transactions?page=${page}&size=${size}`)
      .then(r => json<TransactionList>(r))
  },

  createAccount(data: { externalReference: string; currency: string; initialBalance?: number }): Promise<Account> {
    return fetch(`${BASE}/accounts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }).then(r => json<Account>(r))
  },

  /**
   * Transfers funds. Always generates a fresh `idempotencyKey` per submit so
   * double-clicks and network retries don't double-charge the account.
   * Callers may override the key (e.g. for an explicit retry of a known-failed
   * submit) by passing `idempotencyKey` in `data`.
   */
  transfer(
    accountId: string,
    data: {
      amount: number
      type: 'CREDIT' | 'DEBIT'
      description: string
      idempotencyKey?: string
    }
  ): Promise<Transaction> {
    const body = {
      ...data,
      idempotencyKey: data.idempotencyKey ?? crypto.randomUUID(),
    }
    return fetch(`${BASE}/accounts/${accountId}/transfers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then(r => json<Transaction>(r))
  },
}
