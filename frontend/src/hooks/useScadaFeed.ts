import { useCallback, useEffect, useState } from 'react'
import { errorMessage, fetchDashboard } from '../api'
import type { ConnectionState, DashboardSnapshot } from '../types'

export interface ScadaFeed {
  snapshot: DashboardSnapshot | null
  connection: ConnectionState
  error: string | null
  refresh: () => void
}

export function useScadaFeed(): ScadaFeed {
  const [snapshot, setSnapshot] = useState<DashboardSnapshot | null>(null)
  const [connection, setConnection] = useState<ConnectionState>('connecting')
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let disposed = false
    let source: EventSource | null = null
    let reconnectTimer: number | undefined
    const controller = new AbortController()

    setConnection('connecting')
    setError(null)

    const connectStream = () => {
      if (disposed) return
      source = new EventSource('/api/stream')

      source.addEventListener('snapshot', (event) => {
        if (disposed) return
        try {
          const next = JSON.parse((event as MessageEvent<string>).data) as DashboardSnapshot
          setSnapshot(next)
          setConnection('live')
          setError(null)
        } catch {
          setError('Telemetry frame could not be decoded')
        }
      })

      source.onerror = () => {
        if (disposed) return
        setConnection('reconnecting')
        if (source?.readyState === EventSource.CLOSED) {
          source.close()
          reconnectTimer = window.setTimeout(connectStream, 3000)
        }
      }

      source.onopen = () => {
        if (!disposed) {
          setConnection((previous) => (previous === 'live' ? previous : 'connecting'))
        }
      }
    }

    fetchDashboard(controller.signal)
      .then((data) => {
        if (!disposed) {
          setSnapshot(data)
          setError(null)
        }
      })
      .catch((reason: unknown) => {
        if (!disposed && (reason as Error)?.name !== 'AbortError') {
          setError(errorMessage(reason))
        }
      })

    connectStream()

    const handleOffline = () => {
      if (!disposed) setConnection('offline')
    }
    const handleOnline = () => {
      if (!disposed) setConnection('reconnecting')
    }
    window.addEventListener('offline', handleOffline)
    window.addEventListener('online', handleOnline)

    return () => {
      disposed = true
      controller.abort()
      source?.close()
      if (reconnectTimer) window.clearTimeout(reconnectTimer)
      window.removeEventListener('offline', handleOffline)
      window.removeEventListener('online', handleOnline)
    }
  }, [attempt])

  const refresh = useCallback(() => setAttempt((value) => value + 1), [])

  return { snapshot, connection, error, refresh }
}
