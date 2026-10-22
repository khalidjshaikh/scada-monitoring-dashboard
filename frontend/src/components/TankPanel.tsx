import type { Tank } from '../types'
import { Icon } from './icons'
import { classNames, formatBarrels, formatNumber } from '../format'

interface TankPanelProps {
  tanks: Tank[]
  averageLevel: number
  totalInventory: number
}

export function TankPanel({ tanks, averageLevel, totalInventory }: TankPanelProps) {
  const groups = new Map<string, Tank[]>()
  for (const tank of tanks) {
    const list = groups.get(tank.siteCode) ?? []
    list.push(tank)
    groups.set(tank.siteCode, list)
  }
  const warningCount = tanks.filter((tank) => tank.levelState !== 'NORMAL').length

  return (
    <section className="panel tank-panel span-4">
      <div className="panel-header tank-header">
        <div className="panel-heading">
          <div className="kicker">
            <span className="kicker-index">04</span>
            <span className="kicker-divider" />
            <span>STORAGE INVENTORY</span>
          </div>
          <h2 className="panel-title">Tank farm</h2>
        </div>
        <div className="tank-summary">
          <strong>{formatBarrels(totalInventory)}</strong>
          <span>{formatNumber(averageLevel, 1)}% weighted fill</span>
        </div>
      </div>

      <div className="tank-groups">
        {[...groups.entries()].map(([siteCode, siteTanks]) => (
          <div className="tank-group" key={siteCode}>
            <div className="tank-group-label">
              <span>{siteCode}</span>
              <em>{siteTanks[0]?.siteName}</em>
              <small>{siteTanks.length} TANKS</small>
            </div>
            <div className="tank-row">
              {siteTanks.map((tank) => (
                <div className="tank-item" key={tank.id}>
                  <div
                    className={classNames('tank-vessel', `level-${tank.levelState.toLowerCase()}`)}
                    title={`${tank.name} · ${tank.product} · ${formatNumber(tank.capacityBarrels)} bbl capacity`}
                  >
                    <span className="tank-grid" />
                    <span className="tank-fill" style={{ height: `${tank.levelPercent}%` }} />
                    <span className="tank-meniscus" style={{ bottom: `${tank.levelPercent}%` }} />
                    <span className="tank-threshold" />
                    <span className="tank-level">
                      {formatNumber(tank.levelPercent, 1)}
                      <small>%</small>
                    </span>
                  </div>
                  <div className="tank-readout">
                    <span className="tank-tag">{tank.tag}</span>
                    <strong>{formatBarrels(tank.volumeBarrels)}</strong>
                    <span>{formatNumber(tank.capacityBarrels / 1000, 0)}k cap</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="panel-footer tank-footer">
        <span>
          <Icon name="droplet" size={12} />
          {warningCount > 0 ? `${warningCount} tank${warningCount > 1 ? 's' : ''} outside reserve band` : 'All tanks inside reserve band'}
        </span>
        <span className="tank-legend">
          <i className="legend-low" /> LOW &lt;15%
          <i className="legend-high" /> HIGH &gt;92%
        </span>
      </div>
    </section>
  )
}
