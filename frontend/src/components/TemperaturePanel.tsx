import type { Site } from '../types'
import { Icon } from './icons'
import { classNames, average, formatNumber } from '../format'

interface TemperaturePanelProps {
  sites: Site[]
}

export function TemperaturePanel({ sites }: TemperaturePanelProps) {
  const readings = sites.map((site) => ({
    site,
    value: site.temperature,
    warning: site.temperatureWarning,
    critical: site.temperatureCritical,
    ratio: site.temperature / site.temperatureCritical,
  }))
  const averageTemp = average(readings.map((reading) => reading.value))
  const hottest = readings.reduce(
    (max, reading) => (reading.value > max.value ? reading : max),
    readings[0] ?? { site: null, value: 0, warning: 0, critical: 0, ratio: 0 },
  )
  const offNormal = readings.filter((reading) => reading.site.temperatureStatus !== 'NORMAL').length

  return (
    <section className="panel temperature-panel span-4">
      <div className="panel-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">06</span>
            <span className="kicker-divider" />
            <span>THERMAL PROFILE</span>
          </div>
          <h2 className="panel-title">Process temperature</h2>
        </div>
        <span className={classNames('temperature-badge', offNormal > 0 && 'is-warning')}>
          <Icon name="thermometer" size={13} />
          {offNormal > 0 ? `${offNormal} EXCURSION${offNormal > 1 ? 'S' : ''}` : 'STABLE'}
        </span>
      </div>

      <div className="temperature-hero">
        <div>
          <span className="hero-label">FIELD AVERAGE</span>
          <div className="hero-value">
            {formatNumber(averageTemp, 1)}
            <span>°C</span>
          </div>
        </div>
        <div className="hero-right">
          <span className="hero-label">HIGHEST READING</span>
          <strong>
            {formatNumber(hottest.value, 1)} °C
            <small>{hottest.site?.code ?? '—'}</small>
          </strong>
          <span className="hero-limit">
            {hottest.site
              ? `${formatNumber(hottest.warning, 0)}° warn / ${formatNumber(hottest.critical, 0)}° trip`
              : 'Awaiting telemetry'}
          </span>
        </div>
      </div>

      <div className="temperature-list">
        {readings.map(({ site, value, warning, critical }) => {
          const scaleMax = critical * 1.08
          const fill = (value / scaleMax) * 100
          const warnMark = (warning / scaleMax) * 100
          return (
            <div className="temperature-row" key={site.id}>
              <div className="temperature-row-head">
                <span className="temperature-site">
                  <b>{site.code}</b>
                  <em>{site.name}</em>
                </span>
                <span className={classNames('temperature-value', `temp-${site.temperatureStatus.toLowerCase()}`)}>
                  {formatNumber(value, 1)}°C
                </span>
              </div>
              <div
                className={classNames('temperature-track', `temp-${site.temperatureStatus.toLowerCase()}`)}
                title={`${site.name} · warning ${formatNumber(warning, 0)} °C · trip ${formatNumber(critical, 0)} °C`}
              >
                <span className="temperature-fill" style={{ width: `${Math.min(100, fill)}%` }} />
                <span className="temperature-mark" style={{ left: `${warnMark}%` }} />
              </div>
            </div>
          )
        })}
      </div>

      <div className="temperature-footer">
        <div className="temperature-legend">
          <span><i className="legend-normal" />Normal</span>
          <span><i className="legend-warning" />Warning</span>
          <span><i className="legend-trip" />Trip</span>
        </div>
        <span className="temperature-note">TT probes · 1 s scan</span>
      </div>
    </section>
  )
}
