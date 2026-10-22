import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { acknowledgeAlarm, acknowledgeAllAlarms, errorMessage, sendEquipmentCommand } from './api'
import { Header } from './components/Header'
import { KpiStrip } from './components/KpiStrip'
import { TrendPanel } from './components/TrendPanel'
import { AlarmPanel } from './components/AlarmPanel'
import { Schematic } from './components/Schematic'
import { TankPanel } from './components/TankPanel'
import { EquipmentPanel } from './components/EquipmentPanel'
import { TemperaturePanel } from './components/TemperaturePanel'
import { LoadingState } from './components/LoadingState'
import { CommandModal, type CommandIntent } from './components/CommandModal'
import { ToastStack, type ToastItem, type ToastTone } from './components/ToastStack'
import { useScadaFeed } from './hooks/useScadaFeed'
import { useTrends } from './hooks/useTrends'
import { useNow } from './hooks/useNow'
import { Icon, LogoMark } from './components/icons'
import { formatNumber, formatUptime } from './format'

export default function App() {
  const { snapshot, connection, error, refresh } = useScadaFeed()
  const now = useNow(1000)
  const [minutes, setMinutes] = useState(15)
  const [siteId, setSiteId] = useState<number | null>(null)
  const [ackBusy, setAckBusy] = useState<number | 'all' | null>(null)
  const [commandIntent, setCommandIntent] = useState<CommandIntent | null>(null)
  const [commandBusy, setCommandBusy] = useState(false)
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const toastSequence = useRef(1)
  const toastTimers = useRef<number[]>([])

  const aggregateTrend = useTrends(15, null, snapshot)
  const chartTrend = useTrends(minutes, siteId, snapshot)

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const pushToast = useCallback(
    (tone: ToastTone, title: string, message: string) => {
      const id = toastSequence.current++
      setToasts((current) => [...current.slice(-3), { id, tone, title, message }])
      const timer = window.setTimeout(() => dismissToast(id), 5600)
      toastTimers.current.push(timer)
    },
    [dismissToast],
  )

  useEffect(() => {
    return () => {
      toastTimers.current.forEach((timer) => window.clearTimeout(timer))
    }
  }, [])

  const handleAcknowledge = useCallback(
    async (id: number) => {
      setAckBusy(id)
      try {
        await acknowledgeAlarm(id)
        pushToast('success', 'Alarm acknowledged', 'Acknowledgement written to the operations journal.')
      } catch (reason) {
        pushToast('error', 'Acknowledgement failed', errorMessage(reason))
      } finally {
        setAckBusy(null)
      }
    },
    [pushToast],
  )

  const handleAcknowledgeAll = useCallback(async () => {
    setAckBusy('all')
    try {
      const response = await acknowledgeAllAlarms()
      pushToast('success', 'Alarm queue updated', response.message)
    } catch (reason) {
      pushToast('error', 'Bulk acknowledgement failed', errorMessage(reason))
    } finally {
      setAckBusy(null)
    }
  }, [pushToast])

  const requestCommand = useCallback((equipment: CommandIntent['equipment'], action: CommandIntent['action']) => {
    setCommandIntent({ equipment, action })
  }, [])

  const cancelCommand = useCallback(() => {
    if (!commandBusy) setCommandIntent(null)
  }, [commandBusy])

  const confirmCommand = useCallback(async () => {
    if (!commandIntent) return
    setCommandBusy(true)
    try {
      const response = await sendEquipmentCommand(commandIntent.equipment.id, commandIntent.action)
      pushToast('success', `${commandIntent.equipment.tag} · command accepted`, response.message)
      setCommandIntent(null)
    } catch (reason) {
      pushToast('error', `${commandIntent.equipment.tag} · command rejected`, errorMessage(reason))
    } finally {
      setCommandBusy(false)
    }
  }, [commandIntent, pushToast])

  const handleSiteSelect = useCallback((nextSiteId: number | null) => {
    setSiteId(nextSiteId)
    if (window.innerWidth < 1180) {
      window.setTimeout(() => {
        document.getElementById('trend-panel')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 60)
    }
  }, [])

  const scrollToAlarms = useCallback(() => {
    document.getElementById('alarms')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [])

  const averageLevel = useMemo(() => {
    if (!snapshot || snapshot.tanks.length === 0) return 0
    const capacity = snapshot.tanks.reduce((sum, tank) => sum + tank.capacityBarrels, 0)
    const volume = snapshot.tanks.reduce((sum, tank) => sum + tank.volumeBarrels, 0)
    return capacity > 0 ? (volume / capacity) * 100 : 0
  }, [snapshot])

  if (!snapshot) {
    return <LoadingState connection={connection} error={error} onRetry={refresh} />
  }

  const showLinkBanner = connection === 'reconnecting' || connection === 'offline' || connection === 'connecting'
  const system = snapshot.system

  return (
    <>
      <div className="app-shell">
        <Header
          now={now}
          connection={connection}
          unacknowledgedAlarms={snapshot.kpis.unacknowledgedAlarms}
          criticalAlarms={snapshot.kpis.criticalAlarms}
          onAlarmsClick={scrollToAlarms}
        />

        {showLinkBanner && (
          <div className={`link-banner link-banner-${connection}`} role="status" aria-live="polite">
            <span className="link-banner-icon"><Icon name="signal" size={14} /></span>
            <span>
              {connection === 'offline'
                ? 'Network offline. Telemetry is frozen at the last received frame.'
                : connection === 'connecting'
                  ? 'Re-establishing secure telemetry uplink…'
                  : 'Telemetry link interrupted. The console is retrying automatically.'}
            </span>
            {(connection === 'offline' || connection === 'reconnecting') && (
              <button type="button" onClick={refresh}>
                Retry now
              </button>
            )}
          </div>
        )}

        <main className="dashboard">
          <div className="dashboard-grid">
            <KpiStrip
              kpis={snapshot.kpis}
              sites={snapshot.sites}
              tanks={snapshot.tanks}
              trend={aggregateTrend.trend}
            />

            <TrendPanel
              trend={chartTrend.trend}
              loading={chartTrend.loading}
              error={chartTrend.error}
              minutes={minutes}
              onMinutesChange={setMinutes}
              sites={snapshot.sites}
              siteId={siteId}
              onSiteChange={setSiteId}
              connection={connection}
            />

            <AlarmPanel
              alarms={snapshot.alarms}
              now={now}
              ackBusy={ackBusy}
              onAcknowledge={handleAcknowledge}
              onAcknowledgeAll={handleAcknowledgeAll}
            />

            <Schematic sites={snapshot.sites} siteId={siteId} onSelect={handleSiteSelect} />

            <TankPanel
              tanks={snapshot.tanks}
              averageLevel={averageLevel}
              totalInventory={snapshot.kpis.totalInventoryBarrels}
            />

            <EquipmentPanel
              equipment={snapshot.equipment}
              now={now}
              onRequestCommand={requestCommand}
            />

            <TemperaturePanel sites={snapshot.sites} />
          </div>
        </main>

        <footer className="app-footer">
          <div className="footer-brand">
            <LogoMark className="footer-mark" />
            <div>
              <strong>MERIDIAN SCADA</strong>
              <span>Pipeline control console · build 1.0.0</span>
            </div>
          </div>
          <div className="footer-center">
            <span><i className="footer-led" /> UPTIME {formatUptime(system.uptimeSeconds)}</span>
            <span>{formatNumber(system.readingsStored)} historian rows</span>
            <span>{system.database}</span>
            <span>{system.runtime}</span>
          </div>
          <div className="footer-right">
            <span>SCAN {(system.tickMs / 1000).toFixed(1)} s</span>
            <span>STREAM 2.0 s</span>
            <span className="footer-build">PB-04 · {snapshot.sites.length} STATIONS</span>
          </div>
        </footer>
      </div>

      <CommandModal
        intent={commandIntent}
        busy={commandBusy}
        onCancel={cancelCommand}
        onConfirm={confirmCommand}
      />
      <ToastStack toasts={toasts} onDismiss={dismissToast} />
    </>
  )
}
