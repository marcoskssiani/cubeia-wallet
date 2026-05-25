import { Wallet, Users, TrendingUp, Zap, RefreshCw, PlusCircle } from 'lucide-react'
import type { Stats } from '../types'

interface Props {
  stats: Stats | null
  sseConnected: boolean
  onCreateAccount: () => void
  onRefresh: () => void
}

export function Header({ stats, sseConnected, onCreateAccount, onRefresh }: Props) {
  return (
    <header className="bg-slate-900 border-b border-slate-800 px-6 py-4">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <Wallet className="text-blue-400" size={28} />
          <div>
            <h1 className="text-xl font-bold text-white">Cubeia - Wallet Dashboard</h1>
            <p className="text-xs text-slate-400">- Real-time monitoring - (by Marcos Cassiani)</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className={`flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full border
            ${sseConnected
              ? 'bg-emerald-950 border-emerald-700 text-emerald-400'
              : 'bg-slate-800 border-slate-600 text-slate-500'}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${sseConnected ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`} />
            {sseConnected ? 'Live' : 'Disconnected'}
          </div>
          <button onClick={onRefresh}
            className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
            title="Refresh">
            <RefreshCw size={16} />
          </button>
          <button onClick={onCreateAccount}
            className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-medium transition-colors">
            <PlusCircle size={16} />
            New Account
          </button>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-4 gap-4">
          <StatCard icon={<Users size={18} />} label="Accounts" value={stats.accountCount.toString()} color="blue" />
          <StatCard icon={<Wallet size={18} />} label="Total Balance" value={`€${(stats.totalBalance / 100).toFixed(2)}`} color="emerald" />
          <StatCard icon={<TrendingUp size={18} />} label="Tx (5 min)" value={stats.recentTransactionCount.toString()} color="violet" />
          <StatCard icon={<Zap size={18} />} label="Connections" value={stats.activeConnections.toString()} color="amber" />
        </div>
      )}
    </header>
  )
}

function StatCard({ icon, label, value, color }: {
  icon: React.ReactNode; label: string; value: string
  color: 'blue' | 'emerald' | 'violet' | 'amber'
}) {
  const colors = {
    blue: 'text-blue-400 bg-blue-950 border-blue-800',
    emerald: 'text-emerald-400 bg-emerald-950 border-emerald-800',
    violet: 'text-violet-400 bg-violet-950 border-violet-800',
    amber: 'text-amber-400 bg-amber-950 border-amber-800',
  }
  return (
    <div className={`flex items-center gap-3 px-4 py-3 rounded-lg border ${colors[color]}`}>
      {icon}
      <div>
        <p className="text-xs text-slate-400">{label}</p>
        <p className="text-lg font-bold text-white">{value}</p>
      </div>
    </div>
  )
}
