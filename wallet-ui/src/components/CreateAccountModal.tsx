import React, { useState } from 'react'
import { X } from 'lucide-react'
import { walletApi } from '../api/walletApi'

const CURRENCIES = ['EUR', 'USD', 'GBP', 'JPY', 'CHF']

interface Props {
  onClose: () => void
  onDone: () => void
}

export function CreateAccountModal({ onClose, onDone }: Props) {
  const [ref, setRef] = useState('')
  const [currency, setCurrency] = useState('EUR')
  const [initialBalance, setInitialBalance] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!ref.trim()) { setError('External reference is required'); return }

    const balanceCents = initialBalance ? Math.round(parseFloat(initialBalance) * 100) : 0
    if (initialBalance && (isNaN(balanceCents) || balanceCents < 0)) {
      setError('Initial balance must be a non-negative number'); return
    }

    setSubmitting(true)
    setError(null)
    try {
      await walletApi.createAccount({
        externalReference: ref.trim(),
        currency,
        ...(balanceCents > 0 ? { initialBalance: balanceCents } : {}),
      })
      onDone()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to create account')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-slate-800 rounded-xl p-6 w-full max-w-md border border-slate-700" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold">Create Account</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white"><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-slate-400 mb-1">External Reference</label>
            <input type="text" value={ref} onChange={e => setRef(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
              placeholder="e.g. player-456" />
          </div>

          <div>
            <label className="block text-sm text-slate-400 mb-1">Currency</label>
            <select value={currency} onChange={e => setCurrency(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500">
              {CURRENCIES.map(c => <option key={c}>{c}</option>)}
            </select>
          </div>

          <div>
            <label className="block text-sm text-slate-400 mb-1">Initial Balance (optional)</label>
            <input type="number" step="0.01" min="0" value={initialBalance} onChange={e => setInitialBalance(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
              placeholder="0.00" />
          </div>

          {error && <p className="text-rose-400 text-sm">{error}</p>}

          <button type="submit" disabled={submitting}
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 rounded-lg font-medium transition-colors disabled:opacity-50">
            {submitting ? 'Creating…' : 'Create Account'}
          </button>
        </form>
      </div>
    </div>
  )
}
