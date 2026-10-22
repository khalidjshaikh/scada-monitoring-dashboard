import { Icon, LogoMark } from './icons'
import type { ConnectionState } from '../types'
import { classNames } from '../format'

interface LoadingStateProps {
  connection: ConnectionState
  error: string | null
  onRetry: () => void
}

export function LoadingState({ connection, error, onRetry }: LoadingStateProps) {
  return (
    <div className="boot-screen">
      <div className="boot-grid" />
      <div className="boot-card">
        <div className="boot-mark-wrap">
          <LogoMark className="boot-mark" />
          <span className="boot-ring" />
        </div>
        <div className="boot-kicker">MERIDIAN · PIPELINE CONTROL</div>
        <h1>Establishing field link</h1>
        <p>
          Synchronizing station telemetry, alarm state and remote asset registry from
          the Permian Basin control plane.
        </p>

        <div className={classNames('boot-progress', error && 'has-error')}>
          <span />
        </div>

        <div className="boot-meta">
          <span><Icon name="signal" size={13} /> RTU-04 / PB-04</span>
          <span className="boot-status">
            {error ? 'LINK ERROR' : connection === 'offline' ? 'OFFLINE' : 'SYNCING HISTORIAN'}
          </span>
        </div>

        {error && (
          <div className="boot-error">
            <Icon name="alert" size={16} />
            <div>
              <strong>Telemetry service unreachable</strong>
              <span>{error}</span>
            </div>
            <button type="button" className="button-secondary" onClick={onRetry}>
              <Icon name="refresh" size={14} />
              Retry
            </button>
          </div>
        )}

        <div className="boot-footer">
          <span>H2 HISTORIAN</span>
          <span>2.5.252</span>
          <span>JAVA 27</span>
          <span className="boot-live-dot" />
          <span>STREAM /api/stream</span>
        </div>
      </div>
    </div>
  )
}
