import { ArrowUpRight, ChevronRight } from 'lucide-react'
import type { Account } from '../types'
import { formatAmount } from '../utils/format'

interface Props {
  accounts: Account[] | null
  selectedId: string | null
  onSelect: (account: Account) => void
  onTransfer: (account: Account) => void
}

export function AccountList({ accounts, selectedId, onSelect, onTransfer }: Props) {
  return (
    <div className="flex flex-col h-full">
      <div className="px-4 py-3 border-b border-slate-800">
        <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">
          Accounts {accounts ? `(${accounts.length})` : ''}
        </h2>
      </div>

      <div className="flex-1 overflow-y-auto">
        {!accounts && (
          <div className="p-4 text-slate-500 text-sm text-center">Loading…</div>
        )}
        {accounts?.length === 0 && (
          <div className="p-4 text-slate-500 text-sm text-center">No accounts yet</div>
        )}
        {accounts?.map(account => (
          <div key={account.id}
            onClick={() => onSelect(account)}
            className={`group flex items-center gap-3 px-4 py-3 cursor-pointer border-b border-slate-800/50 hover:bg-slate-800/50 transition-colors
              ${selectedId === account.id ? 'bg-slate-800 border-l-2 border-l-blue-500' : ''}`}>
            <div className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0
              ${currencyColor(account.currency)}`}>
              {account.externalReference.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">{account.externalReference}</p>
              <p className="text-xs text-slate-400 font-mono">{formatAmount(account.balance, account.currency)}</p>
            </div>
            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={e => { e.stopPropagation(); onTransfer(account) }}
                className="p-1 text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950 rounded"
                title="Credit">
                <ArrowUpRight size={14} />
              </button>
              <ChevronRight size={14} className="text-slate-600" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function currencyColor(currency: string): string {
  const map: Record<string, string> = {
    EUR: 'bg-blue-800 text-blue-200',
    USD: 'bg-emerald-800 text-emerald-200',
    GBP: 'bg-violet-800 text-violet-200',
    JPY: 'bg-rose-800 text-rose-200',
  }
  return map[currency] ?? 'bg-slate-700 text-slate-200'
}
