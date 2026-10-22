import { useEffect, useId, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import type { ConnectionState, Site, TrendPoint, TrendResponse } from '../types'
import { Icon } from './icons'
import { classNames, clamp, formatNumber, formatShortTime, formatTime } from '../format'

interface TrendPanelProps {
  trend: TrendResponse | null
  loading: boolean
  error: string | null
  minutes: number
  onMinutesChange: (minutes: number) => void
  sites: Site[]
  siteId: number | null
  onSiteChange: (siteId: number | null) => void
  connection: ConnectionState
}

interface ChartPoint extends TrendPoint {
  x: number
  yPressure: number
  yFlow: number
}

const CHART_HEIGHT = 286
const TEAL = '#43e2c4'
const AMBER = '#f2b34b'

function smoothPath(points: Array<{ x: number; y: number }>): string {
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0].y}`
  let path = `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`
  for (let index = 1; index < points.length; index++) {
    const previous = points[index - 1]
    const current = points[index]
    const control = (previous.x + current.x) / 2
    path += ` C ${control.toFixed(2)} ${previous.y.toFixed(2)}, ${control.toFixed(2)} ${current.y.toFixed(2)}, ${current.x.toFixed(2)} ${current.y.toFixed(2)}`
  }
  return path
}

function samplePoints(points: TrendPoint[]): TrendPoint[] {
  if (points.length <= 340) return points
  const step = Math.ceil(points.length / 340)
  const sampled = points.filter((_, index) => index % step === 0)
  const last = points[points.length - 1]
  if (sampled[sampled.length - 1] !== last) sampled.push(last)
  return sampled
}

export function TrendPanel({
  trend,
  loading,
  error,
  minutes,
  onMinutesChange,
  sites,
  siteId,
  onSiteChange,
  connection,
}: TrendPanelProps) {
  const gradientId = useId().replace(/:/g, '')
  const chartRef = useRef<HTMLDivElement>(null)
  const svgRef = useRef<SVGSVGElement>(null)
  const [width, setWidth] = useState(0)
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  useEffect(() => {
    const element = chartRef.current
    if (!element) return
    const observer = new ResizeObserver((entries) => {
      const nextWidth = entries[0]?.contentRect.width ?? 0
      setWidth(Math.max(320, Math.round(nextWidth)))
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  const compact = width < 620
  const points = useMemo(() => samplePoints(trend?.points ?? []), [trend])
  const current = points[points.length - 1]
  const selectedSite = sites.find((site) => site.id === siteId)
  const isLive = connection === 'live'

  const geometry = useMemo(() => {
    if (points.length === 0 || width === 0) return null
    const padding = {
      top: 20,
      right: compact ? 20 : 48,
      bottom: 26,
      left: compact ? 40 : 50,
    }
    const plotWidth = width - padding.left - padding.right
    const plotHeight = CHART_HEIGHT - padding.top - padding.bottom

    const times = points.map((point) => new Date(point.timestamp).getTime())
    let timeMin = Math.min(...times)
    let timeMax = Math.max(...times)
    if (timeMax - timeMin < 60_000) {
      timeMax = timeMin + 60_000
    }

    const targetMin = trend?.targetMin ?? 0
    const targetMax = trend?.targetMax ?? 0
    const pressures = points.map((point) => point.pressure).concat([targetMin, targetMax])
    const flows = points.map((point) => point.flow)
    let pressureMin = Math.min(...pressures)
    let pressureMax = Math.max(...pressures)
    const pressureSpread = pressureMax - pressureMin || Math.max(1, pressureMax * 0.1)
    pressureMin = Math.max(0, pressureMin - pressureSpread * 0.2)
    pressureMax = pressureMax + pressureSpread * 0.2
    let flowMin = Math.min(...flows)
    let flowMax = Math.max(...flows)
    const flowSpread = flowMax - flowMin || Math.max(1, flowMax * 0.1)
    flowMin = Math.max(0, flowMin - flowSpread * 0.3)
    flowMax = flowMax + flowSpread * 0.2

    const xScale = (time: number) =>
      padding.left + ((time - timeMin) / (timeMax - timeMin)) * plotWidth
    const yPressure = (value: number) =>
      padding.top + (1 - (value - pressureMin) / (pressureMax - pressureMin)) * plotHeight
    const yFlow = (value: number) =>
      padding.top + (1 - (value - flowMin) / (flowMax - flowMin)) * plotHeight

    const coords: ChartPoint[] = points.map((point) => ({
      ...point,
      x: xScale(new Date(point.timestamp).getTime()),
      yPressure: yPressure(point.pressure),
      yFlow: yFlow(point.flow),
    }))

    const pressurePath = smoothPath(coords.map((point) => ({ x: point.x, y: point.yPressure })))
    const flowPath = smoothPath(coords.map((point) => ({ x: point.x, y: point.yFlow })))
    const baseline = CHART_HEIGHT - padding.bottom
    const areaPath = coords.length > 1
      ? `${pressurePath} L ${coords[coords.length - 1].x.toFixed(2)} ${baseline} L ${coords[0].x.toFixed(2)} ${baseline} Z`
      : ''

    const ticksY = Array.from({ length: 4 }, (_, index) => {
      const ratio = index / 3
      return {
        y: padding.top + ratio * plotHeight,
        pressure: pressureMax - ratio * (pressureMax - pressureMin),
        flow: flowMax - ratio * (flowMax - flowMin),
      }
    })
    const tickCount = compact ? 4 : 5
    const ticksX = Array.from({ length: tickCount }, (_, index) => {
      const ratio = index / (tickCount - 1)
      const time = timeMin + ratio * (timeMax - timeMin)
      return {
        x: padding.left + ratio * plotWidth,
        label: formatShortTime(new Date(time).toISOString()),
      }
    })

    return {
      padding,
      plotWidth,
      plotHeight,
      baseline,
      pressureMin,
      pressureMax,
      flowMin,
      flowMax,
      coords,
      pressurePath,
      flowPath,
      areaPath,
      ticksY,
      ticksX,
      bandTop: yPressure(targetMax),
      bandBottom: yPressure(targetMin),
    }
  }, [points, width, compact, trend?.targetMin, trend?.targetMax])

  const handlePointer = (event: ReactPointerEvent<SVGSVGElement>) => {
    if (!geometry || !svgRef.current || geometry.coords.length === 0) return
    const bounds = svgRef.current.getBoundingClientRect()
    const x = clamp(event.clientX - bounds.left, geometry.padding.left, width - geometry.padding.right)
    let nearest = 0
    let distance = Number.POSITIVE_INFINITY
    geometry.coords.forEach((point, index) => {
      const next = Math.abs(point.x - x)
      if (next < distance) {
        distance = next
        nearest = index
      }
    })
    setHoverIndex(nearest)
  }

  const hovered = hoverIndex !== null ? geometry?.coords[hoverIndex] : null
  const tooltipLeft = hovered && geometry
    ? clamp(hovered.x, 86, width - 86)
    : 0

  const pressureValue = current?.pressure ?? selectedSite?.pressure ?? 0
  const flowValue = current?.flow ?? selectedSite?.flow ?? 0
  const averagePressure = points.length
    ? points.reduce((sum, point) => sum + point.pressure, 0) / points.length
    : 0

  return (
    <section className="panel trend-panel span-8" id="trend-panel">
      <div className="panel-header chart-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">01</span>
            <span className="kicker-divider" />
            <span>FIELD TELEMETRY</span>
          </div>
          <h2 className="panel-title">Line pressure <span className="title-amp">&</span> throughput</h2>
        </div>

        <div className="chart-controls">
          <label className="select-wrap">
            <Icon name="layers" size={14} />
            <span className="sr-only">Station focus</span>
            <select
              value={siteId ?? 'all'}
              onChange={(event) => {
                const value = event.target.value
                onSiteChange(value === 'all' ? null : Number(value))
              }}
            >
              <option value="all">All stations</option>
              {[...sites]
                .sort((a, b) => a.code.localeCompare(b.code))
                .map((site) => (
                  <option key={site.id} value={site.id}>
                    {site.code} · {site.name}
                  </option>
                ))}
            </select>
            <Icon name="chevron" size={12} className="select-chevron" />
          </label>

          <div className="segmented" role="group" aria-label="Trend window">
            {[
              { value: 5, label: '5M' },
              { value: 15, label: '15M' },
              { value: 60, label: '1H' },
            ].map((option) => (
              <button
                key={option.value}
                type="button"
                className={classNames('segmented-button', minutes === option.value && 'is-active')}
                onClick={() => onMinutesChange(option.value)}
                aria-pressed={minutes === option.value}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="chart-legend-row">
        <div className="chart-legend">
          <span className="legend-item">
            <i className="legend-swatch swatch-pressure" />
            Pressure
            <b>{formatNumber(pressureValue, 1)} bar</b>
          </span>
          <span className="legend-item">
            <i className="legend-swatch swatch-flow" />
            Flow
            <b>{formatNumber(flowValue, 0)} m³/h</b>
          </span>
          {selectedSite && (
            <span className="legend-station">
              <Icon name="pin" size={12} />
              {selectedSite.code}
            </span>
          )}
        </div>
        <span className={classNames('chart-live', isLive && 'is-live')}>
          <span className="status-dot" />
          {isLive ? 'LIVE' : 'HISTORIAN'} · {minutes}M
        </span>
      </div>

      <div className="chart-plot" ref={chartRef}>
        {width > 0 && geometry && geometry.coords.length > 0 && (
          <svg
            ref={svgRef}
            width={width}
            height={CHART_HEIGHT}
            viewBox={`0 0 ${width} ${CHART_HEIGHT}`}
            role="img"
            aria-label={`Line pressure and flow trend for the last ${minutes} minutes`}
            onPointerMove={handlePointer}
            onPointerLeave={() => setHoverIndex(null)}
          >
            <defs>
              <linearGradient id={`pressure-fill-${gradientId}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={TEAL} stopOpacity="0.24" />
                <stop offset="72%" stopColor={TEAL} stopOpacity="0.04" />
                <stop offset="100%" stopColor={TEAL} stopOpacity="0" />
              </linearGradient>
              <filter id={`glow-${gradientId}`} x="-20%" y="-40%" width="140%" height="180%">
                <feGaussianBlur stdDeviation="3" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            {geometry.bandBottom > geometry.bandTop && (
              <>
                <rect
                  x={geometry.padding.left}
                  y={geometry.bandTop}
                  width={geometry.plotWidth}
                  height={geometry.bandBottom - geometry.bandTop}
                  fill={TEAL}
                  fillOpacity="0.035"
                />
                <line
                  x1={geometry.padding.left}
                  x2={width - geometry.padding.right}
                  y1={geometry.bandTop}
                  y2={geometry.bandTop}
                  stroke={TEAL}
                  strokeOpacity="0.18"
                  strokeDasharray="3 5"
                />
                <line
                  x1={geometry.padding.left}
                  x2={width - geometry.padding.right}
                  y1={geometry.bandBottom}
                  y2={geometry.bandBottom}
                  stroke={TEAL}
                  strokeOpacity="0.18"
                  strokeDasharray="3 5"
                />
              </>
            )}

            {geometry.ticksY.map((tick) => (
              <line
                key={tick.y}
                x1={geometry.padding.left}
                x2={width - geometry.padding.right}
                y1={tick.y}
                y2={tick.y}
                stroke="#9fb6bf"
                strokeOpacity="0.075"
              />
            ))}

            {geometry.ticksY.map((tick) => (
              <g key={`labels-${tick.y}`}>
                <text
                  x={geometry.padding.left - 9}
                  y={tick.y + 3.4}
                  textAnchor="end"
                  className="chart-axis-label"
                  fill={TEAL}
                  fillOpacity="0.62"
                >
                  {formatNumber(tick.pressure, 0)}
                </text>
                {!compact && (
                  <text
                    x={width - geometry.padding.right + 9}
                    y={tick.y + 3.4}
                    textAnchor="start"
                    className="chart-axis-label"
                    fill={AMBER}
                    fillOpacity="0.6"
                  >
                    {formatNumber(tick.flow, 0)}
                  </text>
                )}
              </g>
            ))}

            {geometry.ticksX.map((tick) => (
              <g key={tick.x}>
                <line
                  x1={tick.x}
                  x2={tick.x}
                  y1={geometry.padding.top}
                  y2={CHART_HEIGHT - geometry.padding.bottom}
                  stroke="#9fb6bf"
                  strokeOpacity="0.04"
                />
                <text
                  x={tick.x}
                  y={CHART_HEIGHT - 8}
                  textAnchor="middle"
                  className="chart-axis-label"
                  fill="#7f96a2"
                  fillOpacity="0.75"
                >
                  {tick.label}
                </text>
              </g>
            ))}

            <path d={geometry.areaPath} fill={`url(#pressure-fill-${gradientId})`} />
            <path
              d={geometry.flowPath}
              fill="none"
              stroke={AMBER}
              strokeWidth="1.6"
              strokeOpacity="0.86"
              strokeLinejoin="round"
            />
            <path
              d={geometry.pressurePath}
              fill="none"
              stroke={TEAL}
              strokeWidth="2"
              strokeLinejoin="round"
              filter={`url(#glow-${gradientId})`}
            />

            <circle
              cx={geometry.coords[geometry.coords.length - 1].x}
              cy={geometry.coords[geometry.coords.length - 1].yPressure}
              r="8"
              fill={TEAL}
              fillOpacity="0.1"
            />
            <circle
              cx={geometry.coords[geometry.coords.length - 1].x}
              cy={geometry.coords[geometry.coords.length - 1].yPressure}
              r="3"
              fill="#081217"
              stroke={TEAL}
              strokeWidth="1.6"
            />

            {hovered && (
              <g>
                <line
                  x1={hovered.x}
                  x2={hovered.x}
                  y1={geometry.padding.top}
                  y2={CHART_HEIGHT - geometry.padding.bottom}
                  stroke="#c9dbe2"
                  strokeOpacity="0.24"
                  strokeDasharray="3 3"
                />
                <circle cx={hovered.x} cy={hovered.yPressure} r="4" fill="#071115" stroke={TEAL} strokeWidth="2" />
                <circle cx={hovered.x} cy={hovered.yFlow} r="3.4" fill="#071115" stroke={AMBER} strokeWidth="1.8" />
              </g>
            )}
          </svg>
        )}

        {(!geometry || geometry.coords.length === 0) && !loading && (
          <div className="chart-empty">
            <Icon name="pulse" size={24} />
            <strong>{error ? 'Historian unavailable' : 'Waiting for telemetry samples'}</strong>
            <span>{error ?? 'The trend will populate as the field gateway reports.'}</span>
          </div>
        )}

        {loading && (
          <div className="chart-loading">
            <span className="button-spinner" />
            <span>Loading historian window…</span>
          </div>
        )}

        {hovered && (
          <div className="chart-tooltip" style={{ left: tooltipLeft }}>
            <div className="tooltip-time">{formatTime(hovered.timestamp)}</div>
            <div className="tooltip-row">
              <i className="legend-swatch swatch-pressure" />
              <span>Pressure</span>
              <b>{formatNumber(hovered.pressure, 2)} bar</b>
            </div>
            <div className="tooltip-row">
              <i className="legend-swatch swatch-flow" />
              <span>Flow</span>
              <b>{formatNumber(hovered.flow, 1)} m³/h</b>
            </div>
            <div className="tooltip-row tooltip-temperature">
              <Icon name="thermometer" size={12} />
              <span>Temperature</span>
              <b>{formatNumber(hovered.temperature, 1)} °C</b>
            </div>
          </div>
        )}
      </div>

      <div className="chart-footer">
        <span className="chart-footer-band">
          <i />
          Normal envelope {formatNumber(trend?.targetMin ?? 0, 0)}–{formatNumber(trend?.targetMax ?? 0, 0)} bar
        </span>
        <span>AVG {formatNumber(averagePressure, 1)} bar</span>
        <span className="chart-footer-source">
          <Icon name="terminal" size={12} />
          H2 historian · 5 s persistence
        </span>
      </div>
    </section>
  )
}
