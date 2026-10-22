import { useId } from 'react'
import type { Site, TrendResponse } from '../types'
import { Icon } from './icons'
import { classNames, average, formatNumber, formatShortTime } from '../format'

interface TemperaturePanelProps {
  sites: Site[]
  trend: TrendResponse | null
}

export function TemperaturePanel({ sites, trend }: TemperaturePanelProps) {
  const gradientId = useId().replace(/:/g, '')
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
  const trendPoints = trend?.points.slice(-120) ?? []
  const temperatures = trendPoints.map((point) => point.temperature)
  const minTrend = temperatures.length > 0 ? Math.min(...temperatures) : 0
  const maxTrend = temperatures.length > 0 ? Math.max(...temperatures) : 0
  const chartWidth = 360
  const chartHeight = 72
  const spread = maxTrend - minTrend || 1
  const chartCoords = trendPoints.map((point, index) => ({
    x: trendPoints.length > 1 ? (index / (trendPoints.length - 1)) * chartWidth : chartWidth,
    y: 5 + (1 - (point.temperature - minTrend) / spread) * (chartHeight - 10),
  }))
  const chartLine = chartCoords
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)},${point.y.toFixed(2)}`)
    .join(' ')
  const chartArea = chartCoords.length > 1
    ? `${chartLine} L${chartWidth},${chartHeight} L0,${chartHeight} Z`
    : ''

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

      <div className="temperature-chart-block">
        <div className="temperature-chart-head">
          <span><Icon name="pulse" size={12} /> Field average · 15 min</span>
          <span>{formatNumber(minTrend, 1)}–{formatNumber(maxTrend, 1)} °C</span>
        </div>
        <div className="temperature-chart">
          {chartCoords.length > 1 ? (
            <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} preserveAspectRatio="none" aria-hidden="true">
              <defs>
                <linearGradient id={`temp-fill-${gradientId}`} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#f2b34b" stopOpacity="0.22" />
                  <stop offset="100%" stopColor="#f2b34b" stopOpacity="0" />
                </linearGradient>
              </defs>
              {[0.25, 0.5, 0.75].map((ratio) => (
                <line
                  key={ratio}
                  x1="0"
                  x2={chartWidth}
                  y1={chartHeight * ratio}
                  y2={chartHeight * ratio}
                  stroke="#9fb6bf"
                  strokeOpacity="0.09"
                  strokeDasharray="2 4"
                  vectorEffect="non-scaling-stroke"
                />
              ))}
              <path d={chartArea} fill={`url(#temp-fill-${gradientId})`} />
              <path
                d={chartLine}
                fill="none"
                stroke="#f2b34b"
                strokeWidth="1.5"
                strokeOpacity="0.8"
                vectorEffect="non-scaling-stroke"
              />
            </svg>
          ) : (
            <span className="temperature-chart-empty">Collecting thermal samples…</span>
          )}
        </div>
        <div className="temperature-chart-axis">
          <span>{trendPoints[0] ? formatShortTime(trendPoints[0].timestamp) : '—'}</span>
          <span className="temperature-chart-live"><i />LIVE</span>
          <span>{trendPoints[trendPoints.length - 1] ? formatShortTime(trendPoints[trendPoints.length - 1].timestamp) : '—'}</span>
        </div>
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
