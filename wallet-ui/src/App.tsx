import { useState, useCallback } from 'react'
import { Header } from './components/Header'
import { AccountList } from './components/AccountList'
import { ActivityFeed } from './components/ActivityFeed'
import { AccountDetail } from './components/AccountDetail'
import { TransferModal } from './components/TransferModal'
import { CreateAccountModal } from './components/CreateAccountModal'
import { usePolling } from './hooks/usePolling'
import { useActivityFeed } from './hooks/useActivityFeed'
import { walletApi } from './api/walletApi'
import type { Account, Stats } from './types'

export default function App() {
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null)
  const [transferTarget, setTransferTarget] = useState<Account | null>(null)
  const [showCreate, setShowCreate] = useState(false)

  const { data: accounts, refresh: refreshAccounts } = usePolling<Account[]>(
    walletApi.listAccounts, 3000)

  const { data: stats, refresh: refreshStats } = usePolling<Stats>(
    walletApi.getStats, 5000)

  const { events, connected } = useActivityFeed()

  const refresh = useCallback(() => {
    refreshAccounts()
    refreshStats()
  }, [refreshAccounts, refreshStats])

  function handleTransferDone() {
    setTransferTarget(null)
    refresh()
  }

  function handleCreateDone() {
    setShowCreate(false)
    refresh()
  }

  return (
    <div className="flex flex-col h-screen bg-slate-950">
      <Header
        stats={stats}
        sseConnected={connected}
        onCreateAccount={() => setShowCreate(true)}
        onRefresh={refresh}
      />

      <div className="flex flex-1 min-h-0">
        {/* Left: account list */}
        <div className="w-72 flex-shrink-0 border-r border-slate-800 bg-slate-900 flex flex-col">
          <AccountList
            accounts={accounts}
            selectedId={selectedAccount?.id ?? null}
            onSelect={setSelectedAccount}
            onTransfer={setTransferTarget}
          />
        </div>

        {/* Center: account detail or empty state */}
        <div className="flex-1 bg-slate-950 flex flex-col min-w-0">
          {selectedAccount ? (
            <AccountDetail
              account={selectedAccount}
              onTransfer={setTransferTarget}
              onBack={() => setSelectedAccount(null)}
            />
          ) : (
            <EmptyState onCreateAccount={() => setShowCreate(true)} />
          )}
        </div>

        {/* Right: live activity feed */}
        <div className="w-80 flex-shrink-0 border-l border-slate-800 bg-slate-900 flex flex-col">
          <ActivityFeed events={events} connected={connected} />
        </div>
      </div>

      {transferTarget && (
        <TransferModal
          account={transferTarget}
          onClose={() => setTransferTarget(null)}
          onDone={handleTransferDone}
        />
      )}

      {showCreate && (
        <CreateAccountModal
          onClose={() => setShowCreate(false)}
          onDone={handleCreateDone}
        />
      )}
    </div>
  )
}

function EmptyState({ onCreateAccount }: { onCreateAccount: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center h-full text-center px-8">
      <div className="w-16 h-16 bg-slate-800 rounded-2xl flex items-center justify-center mb-4">
        <span className="text-3xl">💳</span>
      </div>
      <h3 className="text-lg font-semibold text-white mb-2">Select an account</h3>
      <p className="text-slate-400 text-sm mb-6 max-w-xs">
        Choose an account from the left panel to view its transaction history and balance,
        or create a new one to get started.
      </p>
      <button onClick={onCreateAccount}
        className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-sm font-medium transition-colors">
        Create Account
      </button>
    </div>
  )
}
