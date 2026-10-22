import { useId } from 'react'

interface SparklineProps {
  data: number[]
  color?: string
  width?: number
  height?: number
  className?: string
}

export function Sparkline({
  data,
  color = '#43e2c4',
  width = 116,
  height = 36,
  className,
}: SparklineProps) {
  const gradientId = useId().replace(/:/g, '')
  const clean = data.filter((value) => Number.isFinite(value))
  const fallback = clean.length > 0 ? clean : [0, 0]
  const min = Math.min(...fallback)
  const max = Math.max(...fallback)
  const spread = max - min || 1
  const padY = 3
  const step = fallback.length > 1 ? width / (fallback.length - 1) : width
  const coords = fallback.map((value, index) => ({
    x: index * step,
    y: padY + (1 - (value - min) / spread) * (height - padY * 2),
  }))

  const line = coords
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(2)},${point.y.toFixed(2)}`)
    .join(' ')
  const area = `${line} L${width},${height} L0,${height} Z`
  const last = coords[coords.length - 1]

  return (
    <svg
      className={className}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.28" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={area} fill={`url(#${gradientId})`} />
      <path
        d={line}
        stroke={color}
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        vectorEffect="non-scaling-stroke"
      />
      <circle cx={last.x} cy={last.y} r="2.1" fill="#081116" stroke={color} strokeWidth="1.2" />
    </svg>
  )
}
