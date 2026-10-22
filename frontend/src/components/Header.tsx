import { Icon, LogoMark } from './icons'
import type { ConnectionState } from '../types'
import { classNames, formatClock, formatDate, timeZoneName } from '../format'

interface HeaderProps {
  now: number
  connection: ConnectionState
  unacknowledgedAlarms: number
  criticalAlarms: number
  onAlarmsClick: () => void
}

const connectionLabel: Record<ConnectionState, string> = {
  connecting: 'SYNCING',
  live: 'UPLINK LIVE',
  reconnecting: 'RELINKING',
  offline: 'OFFLINE',
}

export function Header({
  now,
  connection,
  unacknowledgedAlarms,
  criticalAlarms,
  onAlarmsClick,
}: HeaderProps) {
  const date = new Date(now)
  return (
    <header className="app-header">
      <div className="header-inner">
        <div className="brand">
          <LogoMark className="brand-mark" />
          <div className="brand-copy">
            <div className="brand-name">MERIDIAN</div>
            <div className="brand-sub">PIPELINE CONTROL · PERMIAN BASIN</div>
          </div>
          <div className="header-separator header-network">
            <Icon name="pin" size={13} />
            <span>PB-04 / 212 MI</span>
          </div>
        </div>

        <div className="header-right">
          <div className="header-clock">
            <span className="clock-time">{formatClock(date)}</span>
            <span className="clock-date">
              {formatDate(date)} · {timeZoneName(date)}
            </span>
          </div>

          <div className={classNames('link-pill', `link-${connection}`)} role="status" aria-live="polite">
            <span className="status-dot" />
            {connectionLabel[connection]}
          </div>

          <button
            type="button"
            className={classNames('header-alarm', criticalAlarms > 0 && 'has-critical')}
            onClick={onAlarmsClick}
            aria-label={`${unacknowledgedAlarms} unacknowledged alarms — jump to alarm queue`}
            title="Jump to alarm queue"
          >
            <Icon name="bell" size={18} />
            {unacknowledgedAlarms > 0 && (
              <span className="header-alarm-count">{unacknowledgedAlarms > 99 ? '99+' : unacknowledgedAlarms}</span>
            )}
          </button>

          <div className="operator-chip">
            <div className="operator-avatar">SA</div>
            <div className="operator-copy">
              <span className="operator-name">SHIFT A</span>
              <span className="operator-role">Control room</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  )
}
