import { useState, useEffect } from 'react'
import type { ActivityEvent } from '../types'

const MAX_EVENTS = 100

export function useActivityFeed() {
  const [events, setEvents] = useState<ActivityEvent[]>([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const es = new EventSource('/api/v1/events')

    es.addEventListener('transaction', (e: MessageEvent) => {
      const event: ActivityEvent = JSON.parse(e.data)
      setEvents(prev => [event, ...prev].slice(0, MAX_EVENTS))
    })

    es.onopen = () => setConnected(true)
    es.onerror = () => setConnected(false)

    return () => {
      es.close()
      setConnected(false)
    }
  }, [])

  return { events, connected }
}
