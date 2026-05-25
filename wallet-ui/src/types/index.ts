export interface Account {
  id: string
  externalReference: string
  currency: string
  balance: number
  createdAt: string
}

export interface Transaction {
  id: string
  accountId: string
  amount: number
  preBalance: number
  postBalance: number
  type: 'CREDIT' | 'DEBIT'
  description: string
  createdAt: string
  idempotencyKey: string | null
}

export interface TransactionList {
  accountId: string
  transactions: Transaction[]
  totalElements: number
  page: number
  size: number
}

export interface Stats {
  accountCount: number
  totalBalance: number
  recentTransactionCount: number
  activeConnections: number
}

export interface ActivityEvent {
  type: string
  transactionId: string
  accountId: string
  externalReference: string
  currency: string
  amount: number
  transactionType: 'CREDIT' | 'DEBIT'
  description: string
  postBalance: number
  timestamp: string
}

export interface TransferFormData {
  amount: string
  type: 'CREDIT' | 'DEBIT'
  description: string
}

export interface CreateAccountFormData {
  externalReference: string
  currency: string
  initialBalance: string
}
