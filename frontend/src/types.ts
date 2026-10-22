export type OperationalStatus = 'NORMAL' | 'WARNING' | 'CRITICAL' | 'OFFLINE'
export type AlarmSeverity = 'CRITICAL' | 'WARNING' | 'INFO'
export type AlarmStatus = 'ACTIVE' | 'ACKNOWLEDGED' | 'CLEARED'
export type EquipmentType = 'PUMP' | 'COMPRESSOR' | 'VALVE'
export type EquipmentState = 'RUNNING' | 'STANDBY' | 'STOPPED' | 'OPEN' | 'CLOSED' | 'FAULT'
export type EquipmentMode = 'REMOTE' | 'LOCAL'
export type TankLevelState = 'CRITICAL_LOW' | 'LOW' | 'NORMAL' | 'HIGH'
export type EquipmentAction = 'START' | 'STOP' | 'OPEN' | 'CLOSE' | 'RESET'

export interface Kpis {
  averagePressure: number
  totalFlow: number
  averageTemperature: number
  averageTankLevel: number
  totalInventoryBarrels: number
  activeAlarms: number
  criticalAlarms: number
  unacknowledgedAlarms: number
  equipmentTotal: number
  equipmentAvailable: number
  equipmentAvailabilityPercent: number
}

export interface Site {
  id: number
  code: string
  name: string
  region: string
  commodity: string
  segment: string
  x: number
  y: number
  pressure: number
  flow: number
  temperature: number
  nominalFlow: number
  pressureLow: number
  pressureHigh: number
  temperatureWarning: number
  status: OperationalStatus
  pressureStatus: OperationalStatus
  flowStatus: OperationalStatus
  temperatureStatus: OperationalStatus
  activeAlarms: number
  equipmentRunning: number
  equipmentTotal: number
  pressureTrend: number[]
}

export interface Tank {
  id: number
  siteId: number
  siteCode: string
  siteName: string
  tag: string
  name: string
  product: string
  capacityBarrels: number
  levelPercent: number
  volumeBarrels: number
  levelState: TankLevelState
}

export interface Equipment {
  id: number
  siteId: number
  siteCode: string
  siteName: string
  tag: string
  name: string
  type: EquipmentType
  state: EquipmentState
  mode: EquipmentMode
  flowControl: boolean
  lastCommand: string | null
  lastCommandAt: string | null
  actions: EquipmentAction[]
}

export interface Alarm {
  id: number
  code: string
  severity: AlarmSeverity
  status: AlarmStatus
  message: string
  siteId: number | null
  siteCode: string | null
  siteName: string | null
  source: string
  raisedAt: string
  acknowledgedAt: string | null
  acknowledgedBy: string | null
  clearedAt: string | null
  ageSeconds: number
}

export interface SystemInfo {
  uptimeSeconds: number
  readingsStored: number
  database: string
  runtime: string
  tickMs: number
}

export interface DashboardSnapshot {
  timestamp: string
  kpis: Kpis
  sites: Site[]
  tanks: Tank[]
  equipment: Equipment[]
  alarms: Alarm[]
  system: SystemInfo
}

export interface TrendPoint {
  timestamp: string
  pressure: number
  flow: number
  temperature: number
}

export interface TrendResponse {
  minutes: number
  siteId: number | null
  points: TrendPoint[]
  targetMin: number
  targetMax: number
  generatedAt: string
}

export type ConnectionState = 'connecting' | 'live' | 'reconnecting' | 'offline'

export interface AcknowledgeResponse {
  acknowledged: number
  message: string
}

export interface CommandResponse {
  accepted: boolean
  message: string
  equipment: Equipment
}
