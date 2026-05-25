import React, { useState } from 'react'
import { X } from 'lucide-react'
import { walletApi } from '../api/walletApi'
import type { Account } from '../types'

interface Props {
  account: Account
  onClose: () => void
  onDone: () => void
}

export function TransferModal({ account, onClose, onDone }: Props) {
  const [type, setType] = useState<'CREDIT' | 'DEBIT'>('CREDIT')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const cents = Math.round(parseFloat(amount) * 100)
    if (isNaN(cents) || cents <= 0) { setError('Enter a valid positive amount'); return }
    if (!description.trim()) { setError('Description is required'); return }

    setSubmitting(true)
    setError(null)
    try {
      await walletApi.transfer(account.id, { amount: cents, type, description: description.trim() })
      onDone()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Transfer failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={onClose}>
      <div className="bg-slate-800 rounded-xl p-6 w-full max-w-md border border-slate-700" onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold">Transfer — {account.externalReference}</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white"><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="flex gap-2">
            {(['CREDIT', 'DEBIT'] as const).map(t => (
              <button key={t} type="button"
                onClick={() => setType(t)}
                className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors
                  ${type === t
                    ? (t === 'CREDIT' ? 'bg-emerald-600 text-white' : 'bg-rose-600 text-white')
                    : 'bg-slate-700 text-slate-300 hover:bg-slate-600'}`}>
                {t}
              </button>
            ))}
          </div>

          <div>
            <label className="block text-sm text-slate-400 mb-1">Amount ({account.currency})</label>
            <input type="number" step="0.01" min="0.01" value={amount} onChange={e => setAmount(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
              placeholder="0.00" />
          </div>

          <div>
            <label className="block text-sm text-slate-400 mb-1">Description</label>
            <input type="text" value={description} onChange={e => setDescription(e.target.value)}
              className="w-full bg-slate-700 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
              placeholder="e.g. Monthly salary" />
          </div>

          {error && <p className="text-rose-400 text-sm">{error}</p>}

          <button type="submit" disabled={submitting}
            className={`w-full py-2.5 rounded-lg font-medium transition-colors
              ${type === 'CREDIT' ? 'bg-emerald-600 hover:bg-emerald-500' : 'bg-rose-600 hover:bg-rose-500'}
              disabled:opacity-50`}>
            {submitting ? 'Processing…' : `${type} ${account.currency}`}
          </button>
        </form>
      </div>
    </div>
  )
}
