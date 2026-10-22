import type { Site } from '../types'
import { Icon } from './icons'
import { classNames, formatNumber } from '../format'

interface SchematicProps {
  sites: Site[]
  siteId: number | null
  onSelect: (siteId: number | null) => void
}

export function Schematic({ sites, siteId, onSelect }: SchematicProps) {
  const ordered = [...sites].sort((a, b) => a.x - b.x)
  const totalFlow = sites.reduce((sum, site) => sum + site.flow, 0)
  const equipmentRunning = sites.reduce((sum, site) => sum + site.equipmentRunning, 0)
  const equipmentTotal = sites.reduce((sum, site) => sum + site.equipmentTotal, 0)
  const alerting = sites.filter((site) => site.status !== 'NORMAL').length

  return (
    <section className="panel schematic-panel span-8">
      <div className="panel-header schematic-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">03</span>
            <span className="kicker-divider" />
            <span>FIELD NETWORK</span>
          </div>
          <h2 className="panel-title">Pipeline schematic</h2>
        </div>
        <div className="network-summary">
          <span className={classNames('network-chip', alerting > 0 && 'has-alert')}>
            <span className="status-dot" />
            {alerting > 0 ? `${alerting} station${alerting > 1 ? 's' : ''} off-normal` : 'All stations normal'}
          </span>
          <span className="network-note"><Icon name="arrow" size={12} /> FLOW E→W</span>
        </div>
      </div>

      <div className="schematic-scroll">
        <div className="schematic-track">
          <div className="pipeline-rail">
            <span className="pipeline-glow" />
            <span className="pipeline-flow" />
            <span className="pipeline-flow pipeline-flow-slow" />
          </div>

          <span className="rail-mile rail-mile-start">MILE 0 · NORTH WELLHEAD</span>
          <span className="rail-mile rail-mile-end">MILE 212 · GULF TERMINAL</span>

          {ordered.map((site, index) => {
            const position = 12 + site.x * 0.76
            const selected = siteId === site.id
            const above = index % 2 === 0
            return (
              <button
                key={site.id}
                type="button"
                className={classNames(
                  'station',
                  above ? 'station-above' : 'station-below',
                  selected && 'is-selected',
                  `station-${site.status.toLowerCase()}`,
                )}
                style={{ left: `${position}%` }}
                onClick={() => onSelect(selected ? null : site.id)}
                aria-pressed={selected}
                title={`${site.name} · ${selected ? 'clear station focus' : 'focus station trend'}`}
              >
                <div className="station-card">
                  <div className="station-topline">
                    <span className="station-code">{site.code}</span>
                    <span className={classNames('station-state', `state-${site.status.toLowerCase()}`)}>
                      <span className="status-dot" />
                      {site.status === 'NORMAL' ? 'NOM' : site.status === 'CRITICAL' ? 'CRIT' : 'WARN'}
                    </span>
                  </div>
                  <div className="station-name">{site.name}</div>
                  <div className="station-metrics">
                    <span className={classNames('station-metric', `metric-${site.pressureStatus.toLowerCase()}`)}>
                      <small>P</small>
                      <strong>{formatNumber(site.pressure, 1)}</strong>
                      <em>bar</em>
                    </span>
                    <span className={classNames('station-metric', `metric-${site.flowStatus.toLowerCase()}`)}>
                      <small>Q</small>
                      <strong>{formatNumber(site.flow, 0)}</strong>
                      <em>m³/h</em>
                    </span>
                  </div>
                  <div className="station-subline">
                    <span className={classNames('temp-reading', site.temperatureStatus !== 'NORMAL' && 'is-warning')}>
                      <Icon name="thermometer" size={11} />
                      {formatNumber(site.temperature, 1)} °C
                    </span>
                    <span>{site.equipmentRunning}/{site.equipmentTotal} assets</span>
                    {site.activeAlarms > 0 && (
                      <span className="station-alarms">
                        <Icon name="bell" size={10} /> {site.activeAlarms}
                      </span>
                    )}
                  </div>
                </div>
                <span className="station-node">
                  <span className="station-node-ring" />
                  <span className="station-node-core" />
                </span>
              </button>
            )
          })}

          <span className="schematic-hint">
            <Icon name="pulse" size={12} />
            Select a station to focus the telemetry trend
          </span>
        </div>
      </div>

      <div className="panel-footer schematic-footer">
        <span>
          <Icon name="flow" size={12} />
          NET FLOW <b>{formatNumber(totalFlow, 0)} m³/h</b>
        </span>
        <span>
          <Icon name="signal" size={12} />
          {equipmentRunning}/{equipmentTotal} assets in service
        </span>
        <span className="schematic-scada">SCADA · 4 S / POLL</span>
      </div>
    </section>
  )
}
