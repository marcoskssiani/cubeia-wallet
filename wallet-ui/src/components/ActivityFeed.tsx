import { Activity } from 'lucide-react'
import type { ActivityEvent } from '../types'
import { formatAmount, formatTime } from '../utils/format'

interface Props {
  events: ActivityEvent[]
  connected: boolean
}

export function ActivityFeed({ events, connected }: Props) {
  return (
    <div className="flex flex-col h-full">
      <div className="px-4 py-3 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Activity size={14} className="text-slate-400" />
          <h2 className="text-sm font-semibold text-slate-300 uppercase tracking-wider">Live Feed</h2>
        </div>
        <span className="text-xs text-slate-500">{events.length} events</span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {!connected && events.length === 0 && (
          <div className="p-4 text-center">
            <p className="text-slate-500 text-sm">Connecting to live feed…</p>
          </div>
        )}

        {events.length === 0 && connected && (
          <div className="p-4 text-center">
            <p className="text-slate-500 text-sm">Waiting for transactions…</p>
          </div>
        )}

        {events.map((event, i) => (
          <div key={`${event.transactionId}-${i}`}
            className="flex items-start gap-3 px-4 py-2.5 border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors">
            <div className={`w-1.5 h-1.5 rounded-full mt-2 flex-shrink-0
              ${event.transactionType === 'CREDIT' ? 'bg-emerald-400' : 'bg-rose-400'}`} />
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-medium text-white truncate">{event.externalReference}</span>
                <span className={`text-xs font-mono flex-shrink-0
                  ${event.transactionType === 'CREDIT' ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {event.transactionType === 'CREDIT' ? '+' : ''}
                  {formatAmount(event.amount, event.currency)}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2 mt-0.5">
                <span className="text-xs text-slate-500 truncate">{event.description}</span>
                <span className="text-xs text-slate-600 flex-shrink-0 font-mono">{formatTime(event.timestamp)}</span>
              </div>
              <p className="text-xs text-slate-600 mt-0.5">
                Balance: {formatAmount(event.postBalance, event.currency)}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
