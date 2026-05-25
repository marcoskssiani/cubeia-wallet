import { ArrowUpRight, ArrowDownLeft, Send, ChevronLeft, ChevronRight } from 'lucide-react'
import type { Account, TransactionList } from '../types'
import { formatAmount, formatDateTime } from '../utils/format'
import { usePolling } from '../hooks/usePolling'
import { walletApi } from '../api/walletApi'
import { useState } from 'react'

interface Props {
  account: Account
  onTransfer: (account: Account) => void
  onBack: () => void
}

export function AccountDetail({ account, onTransfer, onBack }: Props) {
  const [page, setPage] = useState(0)
  const PAGE_SIZE = 15

  const { data: txList } = usePolling<TransactionList>(
    () => walletApi.listTransactions(account.id, page, PAGE_SIZE),
    3000,
  )

  const { data: current } = usePolling<Account>(
    () => walletApi.getAccount(account.id),
    3000,
  )

  const live = current ?? account
  const totalPages = txList ? Math.ceil(txList.totalElements / PAGE_SIZE) : 0

  return (
    <div className="flex flex-col h-full">
      <div className="px-4 py-3 border-b border-slate-800">
        <div className="flex items-center gap-3 mb-3">
          <button onClick={onBack} className="text-slate-400 hover:text-white p-1 hover:bg-slate-700 rounded transition-colors">
            <ChevronLeft size={16} />
          </button>
          <div className="flex-1">
            <h2 className="text-base font-semibold text-white">{live.externalReference}</h2>
            <p className="text-xs text-slate-400">{live.currency} · {live.id.slice(0, 8)}…</p>
          </div>
          <button onClick={() => onTransfer(live)}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 rounded-lg text-xs font-medium transition-colors">
            <Send size={12} /> Transfer
          </button>
        </div>

        <div className="bg-slate-800/60 rounded-lg px-4 py-3 text-center">
          <p className="text-xs text-slate-400 mb-1">Current Balance</p>
          <p className="text-2xl font-bold text-white font-mono">{formatAmount(live.balance, live.currency)}</p>
        </div>
      </div>

      <div className="px-4 py-2 border-b border-slate-800 flex items-center justify-between">
        <span className="text-xs text-slate-400">
          {txList ? `${txList.totalElements} transactions` : 'Loading…'}
        </span>
        {totalPages > 1 && (
          <div className="flex items-center gap-2 text-xs">
            <button disabled={page === 0}
              onClick={() => setPage(p => p - 1)}
              className="p-1 text-slate-400 hover:text-white disabled:opacity-30">
              <ChevronLeft size={14} />
            </button>
            <span className="text-slate-400">{page + 1}/{totalPages}</span>
            <button disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}
              className="p-1 text-slate-400 hover:text-white disabled:opacity-30">
              <ChevronRight size={14} />
            </button>
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto">
        {txList?.transactions.map(tx => (
          <div key={tx.id} className="flex items-center gap-3 px-4 py-2.5 border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors">
            <div className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0
              ${tx.type === 'CREDIT' ? 'bg-emerald-950 text-emerald-400' : 'bg-rose-950 text-rose-400'}`}>
              {tx.type === 'CREDIT'
                ? <ArrowUpRight size={14} />
                : <ArrowDownLeft size={14} />}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-white truncate">{tx.description}</p>
              <p className="text-xs text-slate-500 font-mono">{formatDateTime(tx.createdAt)}</p>
            </div>
            <div className="text-right flex-shrink-0">
              <p className={`text-sm font-mono font-medium ${tx.type === 'CREDIT' ? 'text-emerald-400' : 'text-rose-400'}`}>
                {tx.type === 'CREDIT' ? '+' : ''}{formatAmount(tx.amount, live.currency)}
              </p>
              <p className="text-xs text-slate-500 font-mono">{formatAmount(tx.postBalance, live.currency)}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
