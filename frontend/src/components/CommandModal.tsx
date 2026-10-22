import { useEffect, useRef } from 'react'
import type { Equipment, EquipmentAction } from '../types'
import { Icon } from './icons'
import { classNames } from '../format'

export interface CommandIntent {
  equipment: Equipment
  action: EquipmentAction
}

interface CommandModalProps {
  intent: CommandIntent | null
  busy: boolean
  onCancel: () => void
  onConfirm: () => void
}

const actionCopy: Record<EquipmentAction, { title: string; note: string; tone: string; verb: string }> = {
  START: {
    title: 'Start remote asset',
    note: 'The asset will be brought online and its flow contribution restored to the line.',
    tone: 'safe',
    verb: 'Start asset',
  },
  STOP: {
    title: 'Stop remote asset',
    note: 'Stopping this asset removes its capacity from the flow path. Line throughput will fall until it is restarted.',
    tone: 'caution',
    verb: 'Confirm stop',
  },
  OPEN: {
    title: 'Open isolation valve',
    note: 'The valve will be opened and returned to the active flow path. Confirm the downstream line is lined up.',
    tone: 'safe',
    verb: 'Open valve',
  },
  CLOSE: {
    title: 'Close isolation valve',
    note: 'Closing this valve removes it from the active flow path. Throughput will fall until it is reopened.',
    tone: 'caution',
    verb: 'Confirm close',
  },
  RESET: {
    title: 'Reset latched fault',
    note: 'The fault latch will be cleared and the asset returned to a safe stopped or closed state.',
    tone: 'caution',
    verb: 'Reset fault',
  },
}

export function CommandModal({ intent, busy, onCancel, onConfirm }: CommandModalProps) {
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!intent) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) onCancel()
    }
    document.addEventListener('keydown', onKeyDown)
    document.body.classList.add('modal-open')
    const timer = window.setTimeout(() => confirmRef.current?.focus(), 40)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.classList.remove('modal-open')
      window.clearTimeout(timer)
    }
  }, [intent, busy, onCancel])

  if (!intent) return null

  const { equipment, action } = intent
  const copy = actionCopy[action]

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) onCancel()
      }}
    >
      <div className="command-modal" role="dialog" aria-modal="true" aria-labelledby="command-title">
        <div className={classNames('modal-accent', `tone-${copy.tone}`)} />
        <div className="modal-header">
          <div>
            <div className="kicker">
              <span className="kicker-index">REMOTE COMMAND</span>
              <span className="kicker-divider" />
              <span>CONFIRM AUTHORIZATION</span>
            </div>
            <h2 id="command-title">{copy.title}</h2>
          </div>
          <button type="button" className="icon-button" onClick={onCancel} disabled={busy} aria-label="Close dialog">
            <Icon name="close" size={16} />
          </button>
        </div>

        <div className="modal-body">
          <div className="command-asset">
            <div className="command-asset-icon">
              <Icon
                name={equipment.type === 'PUMP' ? 'pump' : equipment.type === 'VALVE' ? 'valve' : 'compressor'}
                size={22}
              />
            </div>
            <div>
              <strong>{equipment.tag} · {equipment.name}</strong>
              <span>{equipment.siteCode} / {equipment.siteName}</span>
            </div>
            <span className={`command-action action-${action.toLowerCase()}`}>{action}</span>
          </div>

          <p className="command-note">{copy.note}</p>

          <div className="command-facts">
            <div>
              <span>Current state</span>
              <strong>{equipment.state}</strong>
            </div>
            <div>
              <span>Control mode</span>
              <strong>{equipment.mode}</strong>
            </div>
            <div>
              <span>Flow path</span>
              <strong>{equipment.flowControl ? 'MAINLINE' : 'BYPASS'}</strong>
            </div>
          </div>

          <div className="command-caution">
            <Icon name="shield" size={15} />
            Commands execute immediately through the field gateway and are written to the
            operations journal.
          </div>
        </div>

        <div className="modal-actions">
          <button type="button" className="button-secondary" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button
            ref={confirmRef}
            type="button"
            className={classNames('button-primary', `tone-${copy.tone}`)}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? <span className="button-spinner" /> : <Icon name="terminal" size={15} />}
            {busy ? 'Sending…' : copy.verb}
          </button>
        </div>
      </div>
    </div>
  )
}
