import type { ReactNode } from 'react'
import type { Kpis, Site, Tank, TrendResponse } from '../types'
import { Sparkline } from './Sparkline'
import { Icon, type IconName } from './icons'
import { classNames, formatBarrels, formatNumber, percentDelta } from '../format'

interface KpiStripProps {
  kpis: Kpis
  sites: Site[]
  tanks: Tank[]
  trend: TrendResponse | null
}

interface Baseline {
  pressure: number
  flow: number
}

function fiveMinuteBaseline(trend: TrendResponse | null): Baseline | null {
  if (!trend || trend.points.length < 2) return null
  const lastTime = new Date(trend.points[trend.points.length - 1].timestamp).getTime()
  const cutoff = lastTime - 5 * 60_000
  const candidate = trend.points.find((point) => new Date(point.timestamp).getTime() >= cutoff)
  if (!candidate) return null
  const candidateTime = new Date(candidate.timestamp).getTime()
  if (lastTime - candidateTime < 45_000) return null
  return { pressure: candidate.pressure, flow: candidate.flow }
}

interface KpiCardProps {
  label: string
  icon: IconName
  value: string
  unit: string
  tone?: 'default' | 'teal' | 'warning' | 'critical'
  delta?: number | null
  deltaLabel?: string
  note: string
  children: ReactNode
}

function KpiCard({
  label,
  icon,
  value,
  unit,
  tone = 'default',
  delta,
  deltaLabel,
  note,
  children,
}: KpiCardProps) {
  const deltaClass =
    delta === undefined || delta === null
      ? 'delta-neutral'
      : delta > 0
        ? 'delta-up'
        : delta < 0
          ? 'delta-down'
          : 'delta-flat'
  return (
    <article className={classNames('kpi-card', `kpi-${tone}`)}>
      <div className="kpi-top">
        <span className="kpi-label">{label}</span>
        <span className="kpi-icon">
          <Icon name={icon} size={16} />
        </span>
      </div>
      <div className="kpi-value">
        {value}
        <span className="kpi-unit">{unit}</span>
      </div>
      <div className="kpi-bottom">
        <div className="kpi-meta">
          {delta !== undefined && delta !== null && (
            <span className={classNames('kpi-delta', deltaClass)}>
              <span aria-hidden="true">{delta > 0 ? '▲' : delta < 0 ? '▼' : '•'}</span>
              {`${delta > 0 ? '+' : ''}${delta.toFixed(2)}%`}
              <span className="kpi-delta-label">{deltaLabel ?? 'vs 5 min'}</span>
            </span>
          )}
          <span className="kpi-note">{note}</span>
        </div>
        {children}
      </div>
    </article>
  )
}

export function KpiStrip({ kpis, sites, tanks, trend }: KpiStripProps) {
  const pressureData = trend?.points.slice(-46).map((point) => point.pressure) ?? []
  const flowData = trend?.points.slice(-46).map((point) => point.flow) ?? []
  const baseline = fiveMinuteBaseline(trend)
  const pressureDelta = baseline ? percentDelta(kpis.averagePressure, baseline.pressure) : null
  const flowDelta = baseline ? percentDelta(kpis.totalFlow, baseline.flow) : null

  const pressureWarn = sites.some((site) => site.pressureStatus !== 'NORMAL')
  const flowWarn = sites.some((site) => site.flowStatus !== 'NORMAL')
  const tankWarn = tanks.some((tank) => tank.levelState !== 'NORMAL')
  const warningCount = Math.max(0, kpis.activeAlarms - kpis.criticalAlarms)

  return (
    <section className="kpi-grid" aria-label="Key operational indicators">
      <KpiCard
        label="Line pressure"
        icon="gauge"
        value={formatNumber(kpis.averagePressure, 1)}
        unit="bar"
        tone={pressureWarn ? 'warning' : 'teal'}
        delta={pressureDelta}
        note={`${sites.length} station average`}
      >
        <Sparkline className="kpi-sparkline" data={pressureData} color={pressureWarn ? '#f2b34b' : '#43e2c4'} />
      </KpiCard>

      <KpiCard
        label="Throughput"
        icon="flow"
        value={formatNumber(kpis.totalFlow, 0)}
        unit="m³/h"
        tone={flowWarn ? 'warning' : 'teal'}
        delta={flowDelta}
        note="Aggregate line flow"
      >
        <Sparkline className="kpi-sparkline" data={flowData} color={flowWarn ? '#f2b34b' : '#68d0f7'} />
      </KpiCard>

      <KpiCard
        label="Tank inventory"
        icon="tank"
        value={formatNumber(kpis.averageTankLevel, 1)}
        unit="% full"
        tone={tankWarn ? 'warning' : 'default'}
        note={`${formatBarrels(kpis.totalInventoryBarrels)} across ${tanks.length} tanks`}
      >
        <div className="mini-tanks" aria-hidden="true">
          {tanks.map((tank) => (
            <span
              key={tank.id}
              className={classNames('mini-tank', `tank-${tank.levelState.toLowerCase()}`)}
            >
              <i style={{ height: `${Math.max(6, Math.min(100, tank.levelPercent))}%` }} />
            </span>
          ))}
        </div>
      </KpiCard>

      <KpiCard
        label="Open alarms"
        icon="bell"
        value={String(kpis.activeAlarms).padStart(2, '0')}
        unit="active"
        tone={kpis.criticalAlarms > 0 ? 'critical' : kpis.activeAlarms > 0 ? 'warning' : 'teal'}
        note={`${kpis.criticalAlarms} critical · ${kpis.unacknowledgedAlarms} unacknowledged`}
      >
        <div className="alarm-mini" aria-hidden="true">
          <div className="alarm-mini-track">
            <span
              className="alarm-mini-critical"
              style={{
                width: kpis.activeAlarms
                  ? `${(kpis.criticalAlarms / kpis.activeAlarms) * 100}%`
                  : '0%',
              }}
            />
            <span
              className="alarm-mini-warning"
              style={{
                width: kpis.activeAlarms ? `${(warningCount / kpis.activeAlarms) * 100}%` : '0%',
              }}
            />
            {kpis.activeAlarms === 0 && <span className="alarm-mini-clear">ALL CLEAR</span>}
          </div>
          <span className="alarm-mini-label">
            {kpis.equipmentAvailable}/{kpis.equipmentTotal} assets available
          </span>
        </div>
      </KpiCard>
    </section>
  )
}
