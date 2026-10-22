import { useId, type ReactNode, type SVGProps } from 'react'

export type IconName =
  | 'gauge'
  | 'flow'
  | 'tank'
  | 'bell'
  | 'alert'
  | 'pump'
  | 'valve'
  | 'compressor'
  | 'pulse'
  | 'signal'
  | 'thermometer'
  | 'shield'
  | 'check'
  | 'close'
  | 'chevron'
  | 'arrow'
  | 'clock'
  | 'terminal'
  | 'layers'
  | 'droplet'
  | 'refresh'
  | 'pin'

const paths: Record<IconName, ReactNode> = {
  gauge: (
    <>
      <path d="M4.4 17.5a8 8 0 1 1 15.2 0" />
      <path d="M12 13l4.2-3.4" />
      <path d="M3.5 20h17" />
      <circle cx="12" cy="13" r="1.2" />
    </>
  ),
  flow: (
    <>
      <path d="M3 7.5h10.5a2.75 2.75 0 1 0-2.75-2.75" />
      <path d="M3 12h15.2a2.6 2.6 0 1 1-2.6 2.6" />
      <path d="M3 16.5h7.2a2.3 2.3 0 1 1-2.3 2.3" />
    </>
  ),
  tank: (
    <>
      <rect x="5.2" y="5.2" width="13.6" height="15.2" rx="2.2" />
      <path d="M5.5 14c1.8-1.4 3.7-1.4 5.5 0s3.7 1.4 5.5 0c.8-.6 1.6-.9 2.4-1" />
      <path d="M8.4 2.5h7.2" />
    </>
  ),
  bell: (
    <>
      <path d="M6.2 9.8a5.8 5.8 0 0 1 11.6 0c0 4.1 1.5 5.2 2 5.7H4.2c.5-.5 2-1.6 2-5.7Z" />
      <path d="M10 19a2.1 2.1 0 0 0 4 0" />
    </>
  ),
  alert: (
    <>
      <path d="M12 3.7 21 20H3L12 3.7Z" />
      <path d="M12 9.3v4.8" />
      <circle cx="12" cy="17.1" r=".9" fill="currentColor" stroke="none" />
    </>
  ),
  pump: (
    <>
      <circle cx="12" cy="12" r="7.3" />
      <path d="M12 7.4c0 3.6 2 4.6 4.6 4.6-3.6 0-4.6 2-4.6 4.6 0-3.6-2-4.6-4.6-4.6 3.6 0 4.6-2 4.6-4.6Z" />
      <path d="M4.7 12H2.9M21.1 12h-1.8" />
    </>
  ),
  valve: (
    <>
      <path d="M3.5 7.5 11 12l-7.5 4.5v-9Z" />
      <path d="M20.5 7.5 13 12l7.5 4.5v-9Z" />
      <path d="M12 12V6.5h3" />
      <path d="M7.6 18.5h8.8" />
    </>
  ),
  compressor: (
    <>
      <rect x="3.5" y="8.3" width="17" height="9.2" rx="2" />
      <path d="M7.5 8.3V5h7.2v3.3M7 17.5v2.1M17 17.5v2.1M8 12.9h8" />
      <path d="M10.5 12.9v3" />
    </>
  ),
  pulse: (
    <>
      <path d="M2.8 12h3.7l2-5.5 3.6 11 2.4-7 1.8 4h4.9" />
    </>
  ),
  signal: (
    <>
      <circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none" />
      <path d="M7.8 16.2a6 6 0 0 1 0-8.4M16.2 7.8a6 6 0 0 1 0 8.4" />
      <path d="M4.8 19.2a10.2 10.2 0 0 1 0-14.4M19.2 4.8a10.2 10.2 0 0 1 0 14.4" />
    </>
  ),
  thermometer: (
    <>
      <path d="M9.5 13.8V5.6a2.4 2.4 0 0 1 4.8 0v8.2a4.1 4.1 0 1 1-4.8 0Z" />
      <path d="M11.9 8.5v7" />
    </>
  ),
  shield: (
    <>
      <path d="m12 3.2 7.5 2.7v5.7c0 4.4-3 7.4-7.5 9.2-4.5-1.8-7.5-4.8-7.5-9.2V5.9L12 3.2Z" />
      <path d="m8.8 11.7 2.2 2.2 4.2-4.2" />
    </>
  ),
  check: <path d="m4.6 12.4 4.7 4.7 10.1-10" />,
  close: (
    <>
      <path d="m5.4 5.4 13.2 13.2M18.6 5.4 5.4 18.6" />
    </>
  ),
  chevron: <path d="m7.5 9.8 4.5 4.5 4.5-4.5" />,
  arrow: (
    <>
      <path d="M3.8 12h16.4" />
      <path d="m14.7 6.5 5.5 5.5-5.5 5.5" />
    </>
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="8.6" />
      <path d="M12 7.3V12l3.2 2.1" />
    </>
  ),
  terminal: (
    <>
      <rect x="3.4" y="4.6" width="17.2" height="14.8" rx="2.2" />
      <path d="m7.2 9.2 2.8 2.8-2.8 2.8M12 15.2h4.8" />
    </>
  ),
  layers: (
    <>
      <path d="m12 3.4 8.5 4.6-8.5 4.6-8.5-4.6L12 3.4Z" />
      <path d="m4 12 8 4.3 8-4.3M4 16.3l8 4.3 8-4.3" />
    </>
  ),
  droplet: (
    <>
      <path d="M12 3.4c3 3.7 5.4 6.8 5.4 9.8a5.4 5.4 0 0 1-10.8 0c0-3 2.4-6.1 5.4-9.8Z" />
      <path d="M9.8 13.6a2.4 2.4 0 0 0 2.4 2.4" />
    </>
  ),
  refresh: (
    <>
      <path d="M20.2 8.2A8.5 8.5 0 0 0 5.6 6.8L3.4 9.6" />
      <path d="M3.8 4.8v4.8h4.8" />
      <path d="M3.8 15.8a8.5 8.5 0 0 0 14.6 1.4l2.2-2.8" />
      <path d="M20.2 19.2v-4.8h-4.8" />
    </>
  ),
  pin: (
    <>
      <path d="M12 21s6.8-6.4 6.8-11.5a6.8 6.8 0 1 0-13.6 0C5.2 14.6 12 21 12 21Z" />
      <circle cx="12" cy="9.4" r="2.4" />
    </>
  ),
}

interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: IconName
  size?: number
}

export function Icon({ name, size = 18, strokeWidth = 1.55, ...rest }: IconProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...rest}
    >
      {paths[name]}
    </svg>
  )
}

export function LogoMark({ className }: { className?: string }) {
  const id = useId().replace(/:/g, '')
  return (
    <svg viewBox="0 0 48 48" className={className} fill="none" aria-hidden="true">
      <defs>
        <linearGradient id={`logo-a-${id}`} x1="8" y1="6" x2="40" y2="42" gradientUnits="userSpaceOnUse">
          <stop stopColor="#5ff2d8" />
          <stop offset="1" stopColor="#14a996" />
        </linearGradient>
        <linearGradient id={`logo-b-${id}`} x1="12" y1="12" x2="38" y2="36" gradientUnits="userSpaceOnUse">
          <stop stopColor="#67d7ff" />
          <stop offset="1" stopColor="#1fbfa7" />
        </linearGradient>
      </defs>
      <path
        d="M24 3.4 42.2 13v22.6L24 45.2 5.8 35.6V13L24 3.4Z"
        stroke={`url(#logo-a-${id})`}
        strokeOpacity="0.4"
        strokeWidth="1.2"
      />
      <path
        d="M13.4 20.2h9.9M24.5 20.2l8-4.5M24.3 20.8l8.2 7M13.6 20.5v7.2c0 1.3 1 2.3 2.3 2.3h7.9"
        stroke={`url(#logo-b-${id})`}
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <circle cx="13.2" cy="20.2" r="3.1" fill="#071117" stroke="#65efd4" strokeWidth="1.4" />
      <circle cx="33.4" cy="15.2" r="2.6" fill="#071117" stroke="#65efd4" strokeWidth="1.4" />
      <circle cx="33.6" cy="28.3" r="2.6" fill="#071117" stroke="#65efd4" strokeWidth="1.4" />
      <circle cx="24.1" cy="30.1" r="1.8" fill="#65efd4" />
      <circle cx="13.2" cy="20.2" r="1.1" fill="#65efd4" />
    </svg>
  )
}
