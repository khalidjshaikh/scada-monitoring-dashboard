import type {
  AcknowledgeResponse,
  CommandResponse,
  DashboardSnapshot,
  EquipmentAction,
  TrendResponse,
} from './types'

class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
    ...init,
  })
  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const body = (await response.json()) as { message?: string }
      if (body?.message) message = body.message
    } catch {
      // Keep the status-based message when the response is not JSON.
    }
    throw new ApiError(message, response.status)
  }
  return (await response.json()) as T
}

export function fetchDashboard(signal?: AbortSignal): Promise<DashboardSnapshot> {
  return request<DashboardSnapshot>('/api/dashboard', { signal })
}

export function fetchTrends(minutes: number, siteId: number | null, signal?: AbortSignal): Promise<TrendResponse> {
  const query = new URLSearchParams({ minutes: String(minutes) })
  if (siteId !== null) query.set('siteId', String(siteId))
  return request<TrendResponse>(`/api/trends?${query.toString()}`, { signal })
}

export function acknowledgeAlarm(id: number): Promise<unknown> {
  return request(`/api/alarms/${id}/acknowledge`, { method: 'POST' })
}

export function acknowledgeAllAlarms(): Promise<AcknowledgeResponse> {
  return request<AcknowledgeResponse>('/api/alarms/acknowledge-all', { method: 'POST' })
}

export function sendEquipmentCommand(id: number, action: EquipmentAction): Promise<CommandResponse> {
  return request<CommandResponse>(`/api/equipment/${id}/command`, {
    method: 'POST',
    body: JSON.stringify({ action }),
  })
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Unexpected field-bus error'
}
