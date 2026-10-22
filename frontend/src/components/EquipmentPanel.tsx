import { useMemo, useState } from 'react'
import type { Equipment, EquipmentAction, EquipmentType } from '../types'
import { Icon, type IconName } from './icons'
import { classNames, formatNumber, relativeTime } from '../format'

interface EquipmentPanelProps {
  equipment: Equipment[]
  now: number
  onRequestCommand: (equipment: Equipment, action: EquipmentAction) => void
}

type Filter = 'ALL' | EquipmentType

const typeIcon: Record<EquipmentType, IconName> = {
  PUMP: 'pump',
  COMPRESSOR: 'compressor',
  VALVE: 'valve',
}

const actionClass: Record<EquipmentAction, string> = {
  START: 'command-safe',
  STOP: 'command-caution',
  OPEN: 'command-safe',
  CLOSE: 'command-caution',
  RESET: 'command-reset',
}

export function EquipmentPanel({ equipment, now, onRequestCommand }: EquipmentPanelProps) {
  const [filter, setFilter] = useState<Filter>('ALL')
  const sorted = useMemo(() => {
    return [...equipment].sort((left, right) => {
      const faultDelta = Number(right.state === 'FAULT') - Number(left.state === 'FAULT')
      if (faultDelta !== 0) return faultDelta
      const modeDelta = Number(right.mode === 'LOCAL') - Number(left.mode === 'LOCAL')
      if (modeDelta !== 0) return modeDelta
      return left.siteCode.localeCompare(right.siteCode) || left.tag.localeCompare(right.tag)
    })
  }, [equipment])
  const counts = useMemo(() => {
    const base: Record<Filter, number> = {
      ALL: equipment.length,
      PUMP: 0,
      COMPRESSOR: 0,
      VALVE: 0,
    }
    for (const item of equipment) base[item.type] += 1
    return base
  }, [equipment])
  const available = equipment.filter((item) => item.state !== 'FAULT').length
  const faulted = equipment.length - available
  const local = equipment.filter((item) => item.mode === 'LOCAL').length
  const visible = filter === 'ALL' ? sorted : sorted.filter((item) => item.type === filter)

  const filters: Array<{ value: Filter; label: string }> = [
    { value: 'ALL', label: 'All' },
    { value: 'PUMP', label: 'Pumps' },
    { value: 'COMPRESSOR', label: 'Compressors' },
    { value: 'VALVE', label: 'Valves' },
  ]

  return (
    <section className="panel equipment-panel span-8">
      <div className="panel-header equipment-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">05</span>
            <span className="kicker-divider" />
            <span>REMOTE OPERATIONS</span>
          </div>
          <h2 className="panel-title">Equipment control</h2>
        </div>
        <div className="equipment-summary">
          <span className={classNames('availability-chip', faulted > 0 && 'has-fault')}>
            <Icon name={faulted > 0 ? 'alert' : 'signal'} size={13} />
            {available}/{equipment.length} available
          </span>
          <span className="equipment-local">{local} local</span>
        </div>
      </div>

      <div className="equipment-toolbar">
        <div className="filter-chips" role="group" aria-label="Filter equipment type">
          {filters.map((option) => (
            <button
              key={option.value}
              type="button"
              className={classNames('filter-chip', filter === option.value && 'is-active')}
              onClick={() => setFilter(option.value)}
            >
              {option.label}
              <span>{counts[option.value]}</span>
            </button>
          ))}
        </div>
        <span className="equipment-hint">
          <Icon name="shield" size={12} />
          Commands require confirmation
        </span>
      </div>

      <div className="equipment-scroll">
        <table className="equipment-table">
          <thead>
            <tr>
              <th>Asset</th>
              <th>State</th>
              <th>Mode</th>
              <th>Last command</th>
              <th className="align-right">Remote action</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((item) => (
              <tr key={item.id} className={classNames(item.state === 'FAULT' && 'row-fault')}>
                <td>
                  <div className="asset-cell">
                    <span className="asset-icon">
                      <Icon name={typeIcon[item.type]} size={17} />
                    </span>
                    <div className="asset-copy">
                      <div className="asset-title">
                        <b>{item.tag}</b>
                        <span className="asset-type">{item.type}</span>
                        {item.flowControl && <span className="asset-flow">MAINLINE</span>}
                      </div>
                      <span className="asset-name">{item.name} · {item.siteCode}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="state-cell">
                    <span className={classNames('state-pill', `state-${item.state.toLowerCase()}`)}>
                      <span className="state-led" />
                      {item.state}
                    </span>
                    {!item.flowControl && <span className="bypass-label">bypass</span>}
                  </div>
                </td>
                <td>
                  <span className={classNames('mode-chip', item.mode === 'LOCAL' && 'mode-local')}>
                    {item.mode}
                  </span>
                </td>
                <td>
                  <div className="last-command">
                    <span>{item.lastCommand ?? '—'}</span>
                    <small>
                      {item.lastCommandAt ? relativeTime(item.lastCommandAt, now) : 'no journal entry'}
                    </small>
                  </div>
                </td>
                <td className="align-right">
                  <div className="row-actions">
                    {item.actions.length === 0 && (
                      <span className="no-action">{item.mode === 'LOCAL' ? 'LOCAL ONLY' : 'NO ACTION'}</span>
                    )}
                    {item.actions.map((action) => (
                      <button
                        key={action}
                        type="button"
                        className={classNames('command-button', actionClass[action])}
                        onClick={() => onRequestCommand(item, action)}
                      >
                        {action}
                        <Icon name="arrow" size={11} />
                      </button>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="panel-footer equipment-footer">
        <span>
          <Icon name="layers" size={12} />
          {formatNumber(equipment.length)} remote assets · {faulted} fault{faulted === 1 ? '' : 's'}
        </span>
        <span className="equipment-heartbeat">
          <span className="status-dot" />
          GATEWAY HEARTBEAT · 1.2 s
        </span>
        <span className="footer-code">MODBUS/TCP</span>
      </div>
    </section>
  )
}
