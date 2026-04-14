import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getDashboard, getFactories, getFactoryInfo, getScenarios } from '@/api/dashboard'
import {
  formatNumber,
  formatTimeLabel,
  formatTimestamp,
  getCongestionTone,
  getDelayStatus,
  getInflowStatus,
} from '@/utils/dashboard'
import { dashboardCopy } from '@/utils/dashboardCopy'

function sortBySnapshot(rows) {
  return [...rows].sort((left, right) => new Date(left.snapshotTime) - new Date(right.snapshotTime))
}

function filterRowsThroughSnapshot(rows, snapshotTime) {
  if (!snapshotTime) {
    return rows
  }

  const selectedTime = new Date(snapshotTime).getTime()
  return rows.filter((entry) => new Date(entry.snapshotTime).getTime() <= selectedTime)
}

function pointAt(rows, snapshotTime) {
  return rows.find((entry) => entry.snapshotTime === snapshotTime) ?? null
}

function metricAt(rows, snapshotTime, key) {
  const point = pointAt(rows, snapshotTime)
  return point?.[key] ?? null
}

function averageMetric(rows, key) {
  const values = rows.map((entry) => Number(entry[key])).filter((value) => Number.isFinite(value))
  if (!values.length) {
    return null
  }
  return values.reduce((total, value) => total + value, 0) / values.length
}

function sumRecent(rows, key, windowSize, snapshotTime) {
  const index = rows.findIndex((entry) => entry.snapshotTime === snapshotTime)
  if (index < 0) {
    return null
  }

  const startIndex = Math.max(0, index - windowSize + 1)
  return rows.slice(startIndex, index + 1).reduce((total, entry) => total + Number(entry[key] ?? 0), 0)
}

export const useDashboardStore = defineStore('dashboard', () => {
  const factories = ref([])
  const scenarios = ref([])
  const factoryInfo = ref(null)
  const shippingDelay = ref([])
  const orderInflow = ref([])
  const robotSummary = ref([])
  const congestion = ref([])

  const selectedFactoryId = ref('')
  const selectedScenarioId = ref('')
  const selectedSnapshot = ref('')
  const locale = ref('en')

  const isBooting = ref(false)
  const isFactoryLoading = ref(false)
  const isDashboardLoading = ref(false)
  const errorMessage = ref('')

  const text = computed(() => dashboardCopy[locale.value] ?? dashboardCopy.en)
  const helpContent = computed(() => text.value.help)

  function clearDashboardData() {
    shippingDelay.value = []
    orderInflow.value = []
    robotSummary.value = []
    congestion.value = []
    selectedSnapshot.value = ''
  }

  function reset() {
    factories.value = []
    scenarios.value = []
    factoryInfo.value = null

    selectedFactoryId.value = ''
    selectedScenarioId.value = ''
    selectedSnapshot.value = ''

    isBooting.value = false
    isFactoryLoading.value = false
    isDashboardLoading.value = false
    errorMessage.value = ''

    clearDashboardData()
  }

  const snapshotOptions = computed(() => {
    const timeline = [...shippingDelay.value, ...orderInflow.value, ...robotSummary.value, ...congestion.value]
    const values = Array.from(new Set(timeline.map((entry) => entry.snapshotTime).filter(Boolean)))
    return values.sort((left, right) => new Date(left) - new Date(right))
  })

  const visibleSnapshotOptions = computed(() =>
    snapshotOptions.value.filter((value) => {
      if (!selectedSnapshot.value) {
        return true
      }
      return new Date(value).getTime() <= new Date(selectedSnapshot.value).getTime()
    }),
  )

  const snapshotLabels = computed(() => visibleSnapshotOptions.value.map((value) => formatTimeLabel(value, locale.value)))
  const visibleShippingDelay = computed(() => filterRowsThroughSnapshot(shippingDelay.value, selectedSnapshot.value))
  const visibleOrderInflow = computed(() => filterRowsThroughSnapshot(orderInflow.value, selectedSnapshot.value))
  const visibleRobotSummary = computed(() => filterRowsThroughSnapshot(robotSummary.value, selectedSnapshot.value))
  const visibleCongestion = computed(() => filterRowsThroughSnapshot(congestion.value, selectedSnapshot.value))

  const selectedShippingDelay = computed(() => pointAt(visibleShippingDelay.value, selectedSnapshot.value))
  const selectedOrderInflow = computed(() => pointAt(visibleOrderInflow.value, selectedSnapshot.value))
  const selectedRobotSummary = computed(() => pointAt(visibleRobotSummary.value, selectedSnapshot.value))
  const selectedCongestion = computed(() => pointAt(visibleCongestion.value, selectedSnapshot.value))

  const delayStatus = computed(() => getDelayStatus(selectedShippingDelay.value?.avgDelayMinutesNext30m, locale.value))
  const inflowStatus = computed(() => getInflowStatus(selectedOrderInflow.value?.orderInflow15m, locale.value))

  const maxCongestionScore = computed(() => {
    const values = visibleCongestion.value.map((entry) => Number(entry.congestionScore)).filter((value) => Number.isFinite(value))
    return values.length ? Math.max(...values) : 0
  })

  const congestionStatus = computed(() => {
    const score = selectedCongestion.value?.congestionScore
    if (score == null) {
      return { label: text.value.noData, tone: 'muted' }
    }
    return { label: formatScore(score), tone: getCongestionTone(score, maxCongestionScore.value) }
  })

  const robotTotals = computed(() => {
    const active = Number(selectedRobotSummary.value?.robotActive ?? 0)
    const idle = Number(selectedRobotSummary.value?.robotIdle ?? 0)
    const charging = Number(selectedRobotSummary.value?.robotCharging ?? 0)
    return { active, idle, charging, total: active + idle + charging }
  })

  const robotSegments = computed(() => {
    const { active, idle, charging, total } = robotTotals.value
    if (!total) {
      return []
    }
    return [
      { key: 'active', label: text.value.active, value: active, share: Math.round((active / total) * 100), angle: (active / total) * 360, color: '#f97316' },
      { key: 'idle', label: text.value.idle, value: idle, share: Math.round((idle / total) * 100), angle: (idle / total) * 360, color: '#0f766e' },
      { key: 'charging', label: text.value.charging, value: charging, share: Math.round((charging / total) * 100), angle: (charging / total) * 360, color: '#2563eb' },
    ]
  })

  const capacityStatus = computed(() => {
    const { idle, charging, total } = robotTotals.value
    if (!total) {
      return { label: text.value.noData, tone: 'muted' }
    }

    const spare = (idle + charging) / total
    if (spare >= 0.42) {
      return { label: locale.value === 'ko' ? '여유' : 'Spare', tone: 'good' }
    }
    if (spare >= 0.26) {
      return { label: locale.value === 'ko' ? '주의' : 'Watch', tone: 'watch' }
    }
    if (spare >= 0.16) {
      return { label: locale.value === 'ko' ? '경고' : 'Warning', tone: 'warning' }
    }
    return { label: locale.value === 'ko' ? '위험' : 'Critical', tone: 'critical' }
  })

  const snapshotCoverage = computed(() => {
    const total = snapshotOptions.value.length
    if (!total) {
      return 0
    }
    return Math.round((visibleSnapshotOptions.value.length / total) * 100)
  })

  const timeWindowLabel = computed(() => {
    if (!selectedSnapshot.value) {
      return text.value.noSnapshotSelected
    }
    const firstSnapshot = visibleSnapshotOptions.value[0] ?? selectedSnapshot.value
    return `${formatTimeLabel(firstSnapshot, locale.value)} - ${formatTimeLabel(selectedSnapshot.value, locale.value)}`
  })

  const selectedSnapshotNumber = computed(() => {
    if (!selectedSnapshot.value) {
      return '-'
    }
    return `${visibleSnapshotOptions.value.length} / ${snapshotOptions.value.length || visibleSnapshotOptions.value.length}`
  })

  const averageDelaySoFar = computed(() => averageMetric(visibleShippingDelay.value, 'avgDelayMinutesNext30m'))
  const averageCongestionSoFar = computed(() => averageMetric(visibleCongestion.value, 'congestionScore'))

  const peakInflowSoFar = computed(() => {
    if (!visibleOrderInflow.value.length) {
      return null
    }
    return visibleOrderInflow.value.reduce((maxValue, entry) => Math.max(maxValue, Number(entry.orderInflow15m ?? 0)), 0)
  })

  const inflowCards = computed(() => ({
    current15m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 1, selectedSnapshot.value),
    current30m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 2, selectedSnapshot.value),
    current60m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 4, selectedSnapshot.value),
  }))

  const delaySeries = computed(() => [{
    key: 'delay',
    label: text.value.shippingDelay,
    color: '#dc2626',
    values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleShippingDelay.value, snapshotTime, 'avgDelayMinutesNext30m')),
  }])

  const inflowSeries = computed(() => [{
    key: 'inflow',
    label: text.value.orderInflow,
    color: '#ea580c',
    values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleOrderInflow.value, snapshotTime, 'orderInflow15m')),
  }])

  const robotSeries = computed(() => [
    { key: 'active', label: text.value.active, color: '#f97316', values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleRobotSummary.value, snapshotTime, 'robotActive')) },
    { key: 'idle', label: text.value.idle, color: '#0f766e', values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleRobotSummary.value, snapshotTime, 'robotIdle')) },
    { key: 'charging', label: text.value.charging, color: '#2563eb', values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleRobotSummary.value, snapshotTime, 'robotCharging')) },
  ])

  const congestionSeries = computed(() => [{
    key: 'congestion',
    label: text.value.congestion,
    color: '#7c3aed',
    values: visibleSnapshotOptions.value.map((snapshotTime) => metricAt(visibleCongestion.value, snapshotTime, 'congestionScore')),
  }])

  function formatMinutes(value) {
    return value == null ? '-' : locale.value === 'ko' ? `${formatNumber(value, 1)}분` : `${formatNumber(value, 1)} min`
  }

  function formatOrders(value) {
    return value == null ? '-' : locale.value === 'ko' ? `${formatNumber(value, 1)}건` : `${formatNumber(value, 1)} orders`
  }

  function formatRobots(value) {
    return value == null ? '-' : locale.value === 'ko' ? `${formatNumber(value)}대` : `${formatNumber(value)} robots`
  }

  function formatScore(value) {
    return value == null ? '-' : locale.value === 'ko' ? `점수 ${formatNumber(value, 1)}` : `score ${formatNumber(value, 1)}`
  }

  let factoryRequest = 0
  let dashboardRequest = 0

  async function loadScenarioDashboard() {
    const currentFactory = selectedFactoryId.value
    const currentScenario = selectedScenarioId.value

    if (!currentFactory || !currentScenario) {
      clearDashboardData()
      return
    }

    const requestId = ++dashboardRequest
    isDashboardLoading.value = true
    errorMessage.value = ''

    try {
      const [shippingDelayRows, orderInflowRows, robotSummaryRows, congestionRows] = await getDashboard(currentFactory, currentScenario)
      if (requestId !== dashboardRequest) return

      shippingDelay.value = sortBySnapshot(shippingDelayRows)
      orderInflow.value = sortBySnapshot(orderInflowRows)
      robotSummary.value = sortBySnapshot(robotSummaryRows)
      congestion.value = sortBySnapshot(congestionRows)

      const snapshots = snapshotOptions.value
      if (!snapshots.includes(selectedSnapshot.value)) {
        selectedSnapshot.value = snapshots.at(-1) ?? ''
      }
    } catch (error) {
      if (requestId !== dashboardRequest) return
      clearDashboardData()
      errorMessage.value = error.message || (locale.value === 'ko' ? '대시보드 데이터를 불러오지 못했습니다.' : 'Failed to load dashboard data.')
    } finally {
      if (requestId !== dashboardRequest) return
      isDashboardLoading.value = false
    }
  }

  async function loadFactoryContext() {
    const currentFactory = selectedFactoryId.value
    if (!currentFactory) {
      factoryInfo.value = null
      scenarios.value = []
      clearDashboardData()
      return
    }

    const requestId = ++factoryRequest
    isFactoryLoading.value = true
    errorMessage.value = ''

    try {
      const [factoryPayload, scenarioPayload] = await Promise.all([
        getFactoryInfo(currentFactory),
        getScenarios(currentFactory),
      ])
      if (requestId !== factoryRequest) return

      factoryInfo.value = factoryPayload
      scenarios.value = scenarioPayload

      if (!scenarioPayload.some((entry) => entry.scenarioId === selectedScenarioId.value)) {
        selectedScenarioId.value = scenarioPayload[0]?.scenarioId ?? ''
      }

      await loadScenarioDashboard()
    } catch (error) {
      if (requestId !== factoryRequest) return
      factoryInfo.value = null
      scenarios.value = []
      clearDashboardData()
      errorMessage.value = error.message || (locale.value === 'ko' ? '공장 데이터를 불러오지 못했습니다.' : 'Failed to load factory data.')
    } finally {
      if (requestId !== factoryRequest) return
      isFactoryLoading.value = false
    }
  }

  async function bootstrap() {
    if (isBooting.value) return
    isBooting.value = true
    errorMessage.value = ''

    try {
      const factoryPayload = await getFactories()
      factories.value = factoryPayload

      if (!factoryPayload.length) {
        selectedFactoryId.value = ''
        selectedScenarioId.value = ''
        await loadFactoryContext()
        return
      }

      if (!factoryPayload.some((entry) => entry.layoutId === selectedFactoryId.value)) {
        selectedFactoryId.value = factoryPayload[0].layoutId
      }

      await loadFactoryContext()
    } catch (error) {
      errorMessage.value = error.message || (locale.value === 'ko' ? '공장 목록을 불러오지 못했습니다.' : 'Failed to load factories.')
    } finally {
      isBooting.value = false
    }
  }

  async function setFactory(factoryId) {
    selectedFactoryId.value = factoryId
    selectedScenarioId.value = ''
    selectedSnapshot.value = ''
    await loadFactoryContext()
  }

  async function setScenario(scenarioId) {
    selectedScenarioId.value = scenarioId
    selectedSnapshot.value = ''
    await loadScenarioDashboard()
  }

  function setSnapshot(snapshotTime) {
    selectedSnapshot.value = snapshotTime
  }

  return {
    // state
    factories,
    scenarios,
    factoryInfo,
    shippingDelay,
    orderInflow,
    robotSummary,
    congestion,
    selectedFactoryId,
    selectedScenarioId,
    selectedSnapshot,
    locale,
    isBooting,
    isFactoryLoading,
    isDashboardLoading,
    errorMessage,

    // copy
    text,
    helpContent,

    // derived
    snapshotOptions,
    visibleSnapshotOptions,
    snapshotLabels,
    visibleShippingDelay,
    visibleOrderInflow,
    visibleRobotSummary,
    visibleCongestion,
    selectedShippingDelay,
    selectedOrderInflow,
    selectedRobotSummary,
    selectedCongestion,
    delayStatus,
    inflowStatus,
    congestionStatus,
    capacityStatus,
    maxCongestionScore,
    robotTotals,
    robotSegments,
    snapshotCoverage,
    timeWindowLabel,
    selectedSnapshotNumber,
    averageDelaySoFar,
    averageCongestionSoFar,
    peakInflowSoFar,
    inflowCards,
    delaySeries,
    inflowSeries,
    robotSeries,
    congestionSeries,

    // helpers
    formatNumber,
    formatMinutes,
    formatOrders,
    formatRobots,
    formatScore,
    formatTimestamp,
    formatTimeLabel,
    metricAt,
    pointAt,
    getDelayStatus,
    getInflowStatus,
    getCongestionTone,

    // actions
    bootstrap,
    loadFactoryContext,
    loadScenarioDashboard,
    setFactory,
    setScenario,
    setSnapshot,
    reset,
  }
})
