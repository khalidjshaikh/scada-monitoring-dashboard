import { useMemo, useState } from 'react'
import type { Alarm } from '../types'
import { Icon } from './icons'
import { classNames, relativeTime } from '../format'

interface AlarmPanelProps {
  alarms: Alarm[]
  now: number
  ackBusy: number | 'all' | null
  onAcknowledge: (id: number) => void
  onAcknowledgeAll: () => void
}

export function AlarmPanel({
  alarms,
  now,
  ackBusy,
  onAcknowledge,
  onAcknowledgeAll,
}: AlarmPanelProps) {
  const [tab, setTab] = useState<'active' | 'history'>('active')
  const active = useMemo(() => alarms.filter((alarm) => alarm.status !== 'CLEARED'), [alarms])
  const unacknowledged = active.filter((alarm) => alarm.status === 'ACTIVE').length
  const critical = active.filter((alarm) => alarm.severity === 'CRITICAL').length
  const visible = tab === 'active' ? active : alarms

  return (
    <section className="panel alarm-panel span-4" id="alarms">
      <div className="panel-header alarm-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">02</span>
            <span className="kicker-divider" />
            <span>ALARM MANAGEMENT</span>
          </div>
          <h2 className="panel-title">Alarm queue</h2>
        </div>
        <div className={classNames('alarm-count', critical > 0 && 'is-critical')}>
          <span>{String(active.length).padStart(2, '0')}</span>
          <small>OPEN</small>
        </div>
      </div>

      <div className="alarm-toolbar">
        <div className="tabs" role="tablist" aria-label="Alarm views">
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'active'}
            className={classNames('tab', tab === 'active' && 'is-active')}
            onClick={() => setTab('active')}
          >
            Active
            <span>{active.length}</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={tab === 'history'}
            className={classNames('tab', tab === 'history' && 'is-active')}
            onClick={() => setTab('history')}
          >
            All events
          </button>
        </div>
        <button
          type="button"
          className="ack-all"
          onClick={onAcknowledgeAll}
          disabled={ackBusy !== null || unacknowledged === 0}
        >
          {ackBusy === 'all' ? <span className="button-spinner" /> : <Icon name="check" size={13} />}
          ACK ALL
        </button>
      </div>

      <div className="alarm-scroll" role="tabpanel">
        {visible.length === 0 && (
          <div className="alarm-empty">
            <span className="empty-icon"><Icon name="shield" size={22} /></span>
            <strong>{tab === 'active' ? 'No active alarms' : 'No alarm history'}</strong>
            <span>All monitored conditions are inside their operating envelope.</span>
          </div>
        )}

        {visible.map((alarm) => {
          const isCleared = alarm.status === 'CLEARED'
          const isAcknowledged = alarm.status === 'ACKNOWLEDGED'
          return (
            <article
              key={alarm.id}
              className={classNames(
                'alarm-row',
                `severity-${alarm.severity.toLowerCase()}`,
                isCleared && 'is-cleared',
                isAcknowledged && 'is-acknowledged',
              )}
            >
              <span className="alarm-glyph">
                <Icon
                  name={alarm.severity === 'INFO' ? 'signal' : 'alert'}
                  size={15}
                />
              </span>
              <div className="alarm-content">
                <p className="alarm-message">{alarm.message}</p>
                <div className="alarm-meta">
                  <span className="alarm-site">{alarm.siteCode ?? 'SYSTEM'}</span>
                  <span className="meta-dot" />
                  <span>{alarm.source}</span>
                  <span className="meta-dot" />
                  <time dateTime={alarm.raisedAt}>{relativeTime(alarm.raisedAt, now)}</time>
                  <span className={classNames('alarm-status', isCleared ? 'cleared' : isAcknowledged ? 'ack' : 'active')}>
                    {isCleared ? 'cleared' : isAcknowledged ? 'ack’d' : 'unack'}
                  </span>
                </div>
                <div className="alarm-submeta">
                  <span>{alarm.code}</span>
                  {alarm.acknowledgedAt && (
                    <span>Ack {relativeTime(alarm.acknowledgedAt, now)} by {alarm.acknowledgedBy}</span>
                  )}
                  {isCleared && alarm.clearedAt && (
                    <span>Cleared {relativeTime(alarm.clearedAt, now)}</span>
                  )}
                </div>
              </div>
              {!isCleared && !isAcknowledged && (
                <button
                  type="button"
                  className="alarm-ack"
                  onClick={() => onAcknowledge(alarm.id)}
                  disabled={ackBusy !== null}
                  aria-label={`Acknowledge ${alarm.code}`}
                >
                  {ackBusy === alarm.id ? <span className="button-spinner" /> : 'ACK'}
                </button>
              )}
              {isAcknowledged && (
                <span className="alarm-ack-state"><Icon name="check" size={12} /> ACK</span>
              )}
            </article>
          )
        })}
      </div>

      <div className="panel-footer alarm-footer">
        <span>
          <Icon name="shield" size={12} />
          {unacknowledged} awaiting operator acknowledgement
        </span>
        <span className="footer-code">ISA 18.2</span>
      </div>
    </section>
  )
}
