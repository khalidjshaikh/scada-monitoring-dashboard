import { useEffect, useState } from 'react'
import { errorMessage, fetchTrends } from '../api'
import { average } from '../format'
import type { DashboardSnapshot, TrendPoint, TrendResponse } from '../types'

export interface TrendState {
  trend: TrendResponse | null
  loading: boolean
  error: string | null
}

function derivePoint(snapshot: DashboardSnapshot, siteId: number | null): TrendPoint | null {
  if (snapshot.sites.length === 0) return null
  const selected = siteId === null ? null : snapshot.sites.find((site) => site.id === siteId)
  if (siteId !== null && !selected) return null
  const pressure = selected
    ? selected.pressure
    : average(snapshot.sites.map((site) => site.pressure))
  const flow = selected
    ? selected.flow
    : snapshot.sites.reduce((sum, site) => sum + site.flow, 0)
  const temperature = selected
    ? selected.temperature
    : average(snapshot.sites.map((site) => site.temperature))
  return {
    timestamp: snapshot.timestamp,
    pressure: Math.round(pressure * 100) / 100,
    flow: Math.round(flow * 10) / 10,
    temperature: Math.round(temperature * 10) / 10,
  }
}

export function useTrends(
  minutes: number,
  siteId: number | null,
  snapshot: DashboardSnapshot | null,
): TrendState {
  const [trend, setTrend] = useState<TrendResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const hasSnapshot = snapshot !== null

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    setTrend(null)
    setLoading(true)
    setError(null)

    fetchTrends(minutes, siteId, controller.signal)
      .then((data) => {
        if (!active) return
        setTrend(data)
        setLoading(false)
      })
      .catch((reason: unknown) => {
        if (!active || (reason as Error)?.name === 'AbortError') return
        setError(errorMessage(reason))
        setLoading(false)
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [minutes, siteId, hasSnapshot])

  useEffect(() => {
    if (!snapshot) return
    setTrend((previous) => {
      if (!previous) return previous
      const point = derivePoint(snapshot, siteId)
      if (!point) return previous
      const pointTime = new Date(point.timestamp).getTime()
      const lastTime = previous.points.length > 0
        ? new Date(previous.points[previous.points.length - 1].timestamp).getTime()
        : 0
      if (pointTime <= lastTime + 400) return previous
      const cutoff = pointTime - minutes * 60_000 - 30_000
      const points = previous.points
        .filter((item) => new Date(item.timestamp).getTime() >= cutoff)
        .concat(point)
        .slice(-1200)
      return { ...previous, points }
    })
  }, [snapshot, minutes, siteId])

  return { trend, loading, error }
}
