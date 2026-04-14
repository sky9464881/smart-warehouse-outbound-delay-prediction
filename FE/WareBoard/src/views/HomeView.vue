<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboard, getFactories, getFactoryInfo, getScenarios } from '@/api/dashboard'
import InfoHint from '@/components/InfoHint.vue'
import RobotDonutChart from '@/components/RobotDonutChart.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TimeSeriesChart from '@/components/TimeSeriesChart.vue'
import {
  formatNumber,
  formatTimestamp,
  formatTimeLabel,
  getCongestionTone,
  getDelayStatus,
  getInflowStatus,
} from '@/utils/dashboard'

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

const copy = {
  en: {
    toggleLabel: 'Language',
    english: 'EN',
    korean: 'KO',
    heroEyebrow: 'Warehouse control room',
    heroTitle: 'Outbound delay dashboard',
    heroText: 'Choose a factory, scenario, and snapshot to inspect the warehouse state. Every chart rewinds to the selected moment so the page reflects only the data that has arrived up to that point.',
    analysisWindow: 'Analysis window',
    snapshotsInView: 'Snapshots in view',
    dataArrivalRule: 'Data arrival rule',
    dataArrivalCopy: 'Data after the selected snapshot is hidden and treated as not yet received.',
    operationalPulse: 'Operational pulse',
    avgDelay: 'Avg delay',
    peakInflow: 'Peak inflow',
    avgCongestion: 'Avg congestion',
    factory: 'Factory',
    scenario: 'Scenario',
    snapshot: 'Snapshot',
    dashboardLoadErrorTitle: 'Dashboard data could not be loaded.',
    reloadDashboard: 'Reload dashboard',
    preparingDashboard: 'Preparing the dashboard...',
    currentContext: 'Current context',
    noData: 'No data',
    active: 'Active',
    idle: 'Idle',
    charging: 'Charging',
    warehouseLayout: 'Warehouse layout',
    noScenarioSelected: 'No scenario selected',
    floorArea: 'Floor area',
    totalRobots: 'Total robots',
    visibleWindow: 'Visible window',
    shippingDelay: 'Shipping delay',
    shippingDelayNote: 'Forecasted average delay for the next 30 minutes at the selected snapshot.',
    orderInflow: 'Order inflow',
    ordersPer15m: 'orders / 15m',
    congestion: 'Congestion',
    score: 'score',
    shippingDelayTrend: 'Shipping delay trend',
    delayBuildUp: 'Delay build-up',
    delayTrendDescription: 'Hover the chart to inspect the nearest value. Future snapshots stay hidden.',
    current: 'Current',
    orderInflowWindows: 'Order inflow windows',
    demandPressure: 'Demand pressure',
    inflowDescription: 'Rolling totals end at the selected snapshot so they reflect only arrived demand.',
    robotStateMix: 'Robot state mix',
    capacityPosture: 'Capacity posture',
    robotDescription: 'The donut shows the selected snapshot. The trend below reveals how the mix arrived there.',
    avgIdle: 'Avg idle',
    robotsLive: 'robots live',
    congestionScore: 'Congestion score',
    flowFriction: 'Flow friction',
    congestionDescription: 'The badge compares the selected point with the highest congestion seen in the visible window.',
    refreshingView: 'Refreshing the selected warehouse view...',
    noSnapshotSelected: 'No snapshot selected',
    help: {
      window: {
        title: 'Analysis window',
        body: 'Charts and status cards use only the data that has arrived up to the selected snapshot.',
        points: ['Data after the selected snapshot is treated as not yet arrived.', 'The visible trend always stops at the selected moment.'],
      },
      shippingDelay: {
        title: 'Shipping delay',
        body: 'Predicted average outbound delay for the next 30 minutes at the selected operating moment.',
        points: ['Higher values mean outbound operations are expected to slow down.', 'Use the trend chart to see whether delay risk is building or recovering.'],
      },
      orderInflow: {
        title: 'Order inflow',
        body: 'Incoming order volume measured every 15 minutes for the selected scenario.',
        points: ['The 30-minute and 1-hour cards are rolling sums ending at the selected snapshot.', 'Spikes often explain later rises in congestion or delay.'],
      },
      robot: {
        title: 'Robot state mix',
        body: 'Current robot distribution at the selected snapshot plus the trend leading up to it.',
        points: ['Active means robots are executing jobs.', 'Idle and charging help explain whether capacity is being fully used.'],
      },
      congestion: {
        title: 'Congestion score',
        body: 'An operational crowding indicator that summarizes movement pressure in the warehouse.',
        points: ['The badge compares the selected point to the peak score seen up to that moment.', 'Use it with inflow and robot states to understand the cause of crowding.'],
      },
      snapshot: {
        title: 'Snapshot',
        body: 'A warehouse state captured on a 15-minute interval.',
        points: ['Selecting an earlier snapshot rewinds the dashboard to that moment.', 'Hover any trend chart to inspect the nearest timestamp value.'],
      },
      factorySelector: {
        title: 'Factory selector',
        body: 'Choose which warehouse layout to inspect.',
        points: ['The summary cards update after the factory changes.', 'Scenario options are filtered to the selected factory.'],
      },
      scenarioSelector: {
        title: 'Scenario selector',
        body: 'A scenario is a distinct operating case within the chosen factory.',
        points: ['Changing the scenario refreshes all trend lines.', 'Snapshot options are rebuilt from the scenario timeline.'],
      },
      currentContext: {
        title: 'Current context',
        body: 'This header keeps the selected factory, scenario, and analysis boundary visible while you compare panels.',
        points: ['Snapshot marks the exact moment represented by the KPI cards.', 'Trend panels below still show the path up to that moment.'],
      },
    },
  },
  ko: {
    toggleLabel: '언어',
    english: 'EN',
    korean: 'KO',
    heroEyebrow: '물류 관제 화면',
    heroTitle: '출고 지연 대시보드',
    heroText: '공장, 시나리오, 스냅샷 시점을 선택해 해당 시점의 창고 상태를 확인합니다. 모든 차트는 선택한 시각으로 되감겨 그때까지 들어온 데이터만 반영합니다.',
    analysisWindow: '분석 구간',
    snapshotsInView: '표시 스냅샷',
    dataArrivalRule: '데이터 반영 규칙',
    dataArrivalCopy: '선택한 스냅샷 이후 데이터는 아직 들어오지 않은 것으로 보고 숨깁니다.',
    operationalPulse: '운영 요약',
    avgDelay: '평균 지연',
    peakInflow: '최대 유입',
    avgCongestion: '평균 혼잡도',
    factory: '공장',
    scenario: '시나리오',
    snapshot: '스냅샷',
    dashboardLoadErrorTitle: '대시보드 데이터를 불러오지 못했습니다.',
    reloadDashboard: '대시보드 다시 불러오기',
    preparingDashboard: '대시보드를 준비하는 중입니다...',
    currentContext: '현재 선택 정보',
    noData: '데이터 없음',
    active: '가동',
    idle: '유휴',
    charging: '충전',
    warehouseLayout: '창고 레이아웃',
    noScenarioSelected: '선택된 시나리오 없음',
    floorArea: '면적',
    totalRobots: '전체 로봇',
    visibleWindow: '표시 구간',
    shippingDelay: '출고 지연',
    shippingDelayNote: '선택한 스냅샷 시점 기준 향후 30분 평균 출고 지연 예측값입니다.',
    orderInflow: '주문 유입량',
    ordersPer15m: '건 / 15분',
    congestion: '혼잡도',
    score: '점수',
    shippingDelayTrend: '출고 지연 추이',
    delayBuildUp: '지연 누적 흐름',
    delayTrendDescription: '차트 위에 커서를 올리면 가장 가까운 시점의 값을 볼 수 있습니다. 미래 시점은 숨겨집니다.',
    current: '현재',
    orderInflowWindows: '주문 유입 구간',
    demandPressure: '수요 압력',
    inflowDescription: '30분, 1시간 집계는 선택한 스냅샷 시점까지 들어온 주문만 합산합니다.',
    robotStateMix: '로봇 상태 구성',
    capacityPosture: '가동 여력',
    robotDescription: '도넛 차트는 선택한 스냅샷의 상태를 보여주고, 아래 추이는 그 상태에 이르기까지의 변화를 보여줍니다.',
    avgIdle: '평균 유휴',
    robotsLive: '가동 로봇',
    congestionScore: '혼잡도 점수',
    flowFriction: '흐름 마찰도',
    congestionDescription: '배지는 현재 선택 시점이 보이는 구간에서 어느 정도 높은 혼잡도인지 비교해 보여줍니다.',
    refreshingView: '선택한 창고 화면을 새로고침하는 중입니다...',
    noSnapshotSelected: '스냅샷이 선택되지 않았습니다',
    help: {
      window: {
        title: '분석 구간',
        body: '차트와 상태 카드는 선택한 스냅샷 시점까지 들어온 데이터만 사용합니다.',
        points: ['선택한 시점 이후 데이터는 아직 도착하지 않은 것으로 처리합니다.', '보이는 추이는 항상 선택한 시점에서 끝납니다.'],
      },
      shippingDelay: {
        title: '출고 지연',
        body: '선택한 운영 시점 기준 향후 30분 평균 출고 지연 예측값입니다.',
        points: ['값이 높을수록 출고 작업이 더 느려질 가능성이 큽니다.', '추이 차트로 지연 위험이 커지는지 완화되는지 볼 수 있습니다.'],
      },
      orderInflow: {
        title: '주문 유입량',
        body: '선택한 시나리오에서 15분 단위로 측정한 주문 유입량입니다.',
        points: ['30분과 1시간 카드는 선택 스냅샷에서 끝나는 누적 합입니다.', '유입량 급증은 이후 혼잡도나 지연 상승의 원인이 될 수 있습니다.'],
      },
      robot: {
        title: '로봇 상태 구성',
        body: '선택한 스냅샷의 현재 로봇 분포와 그 시점까지의 추이를 함께 보여줍니다.',
        points: ['Active는 실제 작업 수행 중인 로봇입니다.', 'Idle과 Charging 비율을 함께 보면 여유 용량을 해석하기 쉽습니다.'],
      },
      congestion: {
        title: '혼잡도 점수',
        body: '창고 이동 압력을 요약한 운영 혼잡 지표입니다.',
        points: ['배지는 보이는 구간 안에서 현재 값이 얼마나 높은지 비교합니다.', '주문 유입량과 로봇 상태를 함께 보면 원인 파악에 도움이 됩니다.'],
      },
      snapshot: {
        title: '스냅샷',
        body: '15분 간격으로 캡처한 창고 상태 시점입니다.',
        points: ['이전 스냅샷을 선택하면 대시보드가 그 시점으로 되감깁니다.', '차트 위에 커서를 올리면 가장 가까운 시점 값을 볼 수 있습니다.'],
      },
      factorySelector: {
        title: '공장 선택',
        body: '확인할 창고 레이아웃을 선택합니다.',
        points: ['공장이 바뀌면 요약 카드가 함께 갱신됩니다.', '시나리오 목록은 선택한 공장 기준으로 필터링됩니다.'],
      },
      scenarioSelector: {
        title: '시나리오 선택',
        body: '선택한 공장 안에서 운영 케이스를 고르는 항목입니다.',
        points: ['시나리오를 바꾸면 모든 추이 차트가 다시 계산됩니다.', '스냅샷 목록도 해당 시나리오 타임라인으로 다시 구성됩니다.'],
      },
      currentContext: {
        title: '현재 선택 정보',
        body: '어떤 공장, 시나리오, 시간대를 보고 있는지 비교 중에도 계속 보여줍니다.',
        points: ['Snapshot은 KPI 카드가 표현하는 정확한 시점을 의미합니다.', '아래 추이 패널은 그 시점에 도달하기까지의 흐름을 보여줍니다.'],
      },
    },
  },
}

const text = computed(() => copy[locale.value])
const helpContent = computed(() => text.value.help)

function sortBySnapshot(rows) {
  return [...rows].sort((left, right) => new Date(left.snapshotTime) - new Date(right.snapshotTime))
}

function clearDashboardData() {
  shippingDelay.value = []
  orderInflow.value = []
  robotSummary.value = []
  congestion.value = []
  selectedSnapshot.value = ''
}

function pointAt(rows, snapshotTime) {
  return rows.find((entry) => entry.snapshotTime === snapshotTime) ?? null
}

function metricAt(rows, snapshotTime, key) {
  const point = pointAt(rows, snapshotTime)
  return point?.[key] ?? null
}

function filterRowsThroughSnapshot(rows, snapshotTime) {
  if (!snapshotTime) {
    return rows
  }

  const selectedTime = new Date(snapshotTime).getTime()
  return rows.filter((entry) => new Date(entry.snapshotTime).getTime() <= selectedTime)
}

function sumRecent(rows, key, windowSize, snapshotTime) {
  const index = rows.findIndex((entry) => entry.snapshotTime === snapshotTime)
  if (index < 0) {
    return null
  }

  const startIndex = Math.max(0, index - windowSize + 1)
  return rows.slice(startIndex, index + 1).reduce((total, entry) => total + Number(entry[key] ?? 0), 0)
}

function averageMetric(rows, key) {
  const values = rows.map((entry) => Number(entry[key])).filter((value) => Number.isFinite(value))
  if (!values.length) {
    return null
  }
  return values.reduce((total, value) => total + value, 0) / values.length
}

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

const maxCongestionScore = computed(() =>
  visibleCongestion.value.reduce((maxValue, entry) => Math.max(maxValue, Number(entry.congestionScore ?? 0)), 0),
)

const congestionStatus = computed(() => {
  const score = selectedCongestion.value?.congestionScore
  if (score == null) {
    return { label: text.value.noData, tone: 'muted' }
  }
  return { label: formatScore(score), tone: getCongestionTone(score, maxCongestionScore.value) }
})

const inflowCards = computed(() => ({
  current15m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 1, selectedSnapshot.value),
  current30m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 2, selectedSnapshot.value),
  current60m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 4, selectedSnapshot.value),
}))

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

const averageDelaySoFar = computed(() => averageMetric(visibleShippingDelay.value, 'avgDelayMinutesNext30m'))
const averageCongestionSoFar = computed(() => averageMetric(visibleCongestion.value, 'congestionScore'))

const peakInflowSoFar = computed(() => {
  if (!visibleOrderInflow.value.length) {
    return null
  }
  return visibleOrderInflow.value.reduce((maxValue, entry) => Math.max(maxValue, Number(entry.orderInflow15m ?? 0)), 0)
})

const selectedSnapshotNumber = computed(() => {
  if (!selectedSnapshot.value) {
    return '-'
  }
  return `${visibleSnapshotOptions.value.length} / ${snapshotOptions.value.length || visibleSnapshotOptions.value.length}`
})

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

async function loadScenarioDashboard() {
  if (!selectedFactoryId.value || !selectedScenarioId.value) {
    clearDashboardData()
    return
  }
  isDashboardLoading.value = true
  errorMessage.value = ''
  try {
    const [shippingDelayRows, orderInflowRows, robotSummaryRows, congestionRows] = await getDashboard(
      selectedFactoryId.value,
      selectedScenarioId.value,
    )
    shippingDelay.value = sortBySnapshot(shippingDelayRows)
    orderInflow.value = sortBySnapshot(orderInflowRows)
    robotSummary.value = sortBySnapshot(robotSummaryRows)
    congestion.value = sortBySnapshot(congestionRows)
    const snapshots = snapshotOptions.value
    if (!snapshots.includes(selectedSnapshot.value)) {
      selectedSnapshot.value = snapshots.at(-1) ?? ''
    }
  } catch (error) {
    clearDashboardData()
    errorMessage.value = error.message || (locale.value === 'ko' ? '대시보드 데이터를 불러오지 못했습니다.' : 'Failed to load dashboard data.')
  } finally {
    isDashboardLoading.value = false
  }
}

async function loadFactoryContext() {
  if (!selectedFactoryId.value) {
    factoryInfo.value = null
    scenarios.value = []
    clearDashboardData()
    return
  }
  isFactoryLoading.value = true
  errorMessage.value = ''
  try {
    const [factoryPayload, scenarioPayload] = await Promise.all([
      getFactoryInfo(selectedFactoryId.value),
      getScenarios(selectedFactoryId.value),
    ])
    factoryInfo.value = factoryPayload
    scenarios.value = scenarioPayload
    if (!scenarioPayload.some((entry) => entry.scenarioId === selectedScenarioId.value)) {
      selectedScenarioId.value = scenarioPayload[0]?.scenarioId ?? ''
    }
    await loadScenarioDashboard()
  } catch (error) {
    factoryInfo.value = null
    scenarios.value = []
    clearDashboardData()
    errorMessage.value = error.message || (locale.value === 'ko' ? '공장 데이터를 불러오지 못했습니다.' : 'Failed to load factory data.')
  } finally {
    isFactoryLoading.value = false
  }
}

async function bootstrapDashboard() {
  isBooting.value = true
  errorMessage.value = ''
  try {
    const factoryPayload = await getFactories()
    factories.value = factoryPayload
    if (!factoryPayload.length) {
      selectedFactoryId.value = ''
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

async function handleFactoryChange(event) {
  selectedFactoryId.value = event.target.value
  selectedScenarioId.value = ''
  selectedSnapshot.value = ''
  await loadFactoryContext()
}

async function handleScenarioChange(event) {
  selectedScenarioId.value = event.target.value
  selectedSnapshot.value = ''
  await loadScenarioDashboard()
}

onMounted(() => {
  bootstrapDashboard()
})
</script>

<template>
  <main class="dashboard-page">
    <section class="hero-shell">
      <div class="hero-copy">
        <div class="hero-topline">
          <p class="eyebrow">{{ text.heroEyebrow }}</p>
          <div class="locale-toggle" role="group" :aria-label="text.toggleLabel">
            <button type="button" class="locale-button" :class="{ 'is-active': locale === 'en' }" @click="locale = 'en'">
              {{ text.english }}
            </button>
            <button type="button" class="locale-button" :class="{ 'is-active': locale === 'ko' }" @click="locale = 'ko'">
              {{ text.korean }}
            </button>
          </div>
        </div>
        <h1>{{ text.heroTitle }}</h1>
        <p class="hero-text">{{ text.heroText }}</p>
        <div class="window-pill-row">
          <article class="window-pill">
            <span>{{ text.analysisWindow }}</span>
            <strong>{{ timeWindowLabel }}</strong>
          </article>
          <article class="window-pill">
            <span>{{ text.snapshotsInView }}</span>
            <strong>{{ selectedSnapshotNumber }}</strong>
          </article>
        </div>
      </div>

      <div class="hero-stack">
        <article class="hero-card">
          <div class="card-topline">
            <p class="card-kicker">{{ text.dataArrivalRule }}</p>
            <InfoHint :title="helpContent.window.title" :body="helpContent.window.body" :points="helpContent.window.points" align="right" />
          </div>
          <strong class="hero-card-value">{{ snapshotCoverage }}%</strong>
          <p class="hero-card-copy">{{ text.dataArrivalCopy }}</p>
          <div class="progress-shell"><span :style="{ width: `${snapshotCoverage}%` }"></span></div>
        </article>

        <article class="hero-card hero-card--accent">
          <p class="card-kicker">{{ text.operationalPulse }}</p>
          <div class="pulse-grid">
            <div><span>{{ text.avgDelay }}</span><strong>{{ formatMinutes(averageDelaySoFar) }}</strong></div>
            <div><span>{{ text.peakInflow }}</span><strong>{{ formatOrders(peakInflowSoFar) }}</strong></div>
            <div><span>{{ text.avgCongestion }}</span><strong>{{ formatScore(averageCongestionSoFar) }}</strong></div>
          </div>
        </article>
      </div>

      <div class="control-grid">
        <label class="field">
          <span class="field-label">
            {{ text.factory }}
            <InfoHint :title="helpContent.factorySelector.title" :body="helpContent.factorySelector.body" :points="helpContent.factorySelector.points" />
          </span>
          <select :value="selectedFactoryId" :disabled="isBooting || isFactoryLoading" @change="handleFactoryChange">
            <option v-for="factory in factories" :key="factory.layoutId" :value="factory.layoutId">{{ factory.layoutId }}</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">
            {{ text.scenario }}
            <InfoHint :title="helpContent.scenarioSelector.title" :body="helpContent.scenarioSelector.body" :points="helpContent.scenarioSelector.points" />
          </span>
          <select :value="selectedScenarioId" :disabled="isBooting || isFactoryLoading || !scenarios.length" @change="handleScenarioChange">
            <option v-for="scenario in scenarios" :key="scenario.scenarioId" :value="scenario.scenarioId">{{ scenario.scenarioId }}</option>
          </select>
        </label>

        <label class="field">
          <span class="field-label">
            {{ text.snapshot }}
            <InfoHint :title="helpContent.snapshot.title" :body="helpContent.snapshot.body" :points="helpContent.snapshot.points" align="right" />
          </span>
          <select v-model="selectedSnapshot" :disabled="isDashboardLoading || !snapshotOptions.length">
            <option v-for="snapshot in snapshotOptions" :key="snapshot" :value="snapshot">{{ formatTimestamp(snapshot, locale) }}</option>
          </select>
        </label>
      </div>
    </section>

    <section v-if="errorMessage" class="message-card is-error">
      <strong>{{ text.dashboardLoadErrorTitle }}</strong>
      <p>{{ errorMessage }}</p>
      <button class="retry-button" type="button" @click="bootstrapDashboard">{{ text.reloadDashboard }}</button>
    </section>

    <section v-else-if="isBooting" class="message-card">
      <p>{{ text.preparingDashboard }}</p>
    </section>

    <template v-else>
      <section class="overview-grid">
        <article class="context-card">
          <div class="card-topline">
            <p class="card-kicker">{{ text.currentContext }}</p>
            <InfoHint :title="helpContent.currentContext.title" :body="helpContent.currentContext.body" :points="helpContent.currentContext.points" align="right" />
          </div>
          <div class="context-main">
            <div>
              <p class="context-label">{{ factoryInfo?.layoutType ?? text.warehouseLayout }}</p>
              <h2>{{ factoryInfo?.layoutId ?? '-' }}</h2>
              <p class="context-subtitle">{{ selectedScenarioId || text.noScenarioSelected }}</p>
            </div>
            <StatusBadge :label="delayStatus.label" :tone="delayStatus.tone" />
          </div>
          <div class="context-meta">
            <span>{{ text.floorArea }} {{ formatNumber(factoryInfo?.floorAreaSqm) }} sqm</span>
            <span>{{ text.totalRobots }} {{ formatNumber(factoryInfo?.robotTotal) }}</span>
            <span>{{ text.snapshot }} {{ formatTimestamp(selectedSnapshot, locale) }}</span>
            <span>{{ text.visibleWindow }} {{ timeWindowLabel }}</span>
          </div>
        </article>

        <article class="metric-card">
          <div class="card-topline">
            <p class="card-kicker">{{ text.shippingDelay }}</p>
            <InfoHint :title="helpContent.shippingDelay.title" :body="helpContent.shippingDelay.body" :points="helpContent.shippingDelay.points" />
          </div>
          <div class="metric-line"><strong>{{ formatNumber(selectedShippingDelay?.avgDelayMinutesNext30m, 1) }}</strong><span>{{ locale === 'ko' ? '분' : 'min' }}</span></div>
          <p class="metric-note">{{ text.shippingDelayNote }}</p>
        </article>

        <article class="metric-card">
          <div class="card-topline">
            <p class="card-kicker">{{ text.orderInflow }}</p>
            <InfoHint :title="helpContent.orderInflow.title" :body="helpContent.orderInflow.body" :points="helpContent.orderInflow.points" />
          </div>
          <div class="metric-line"><strong>{{ formatNumber(selectedOrderInflow?.orderInflow15m, 1) }}</strong><span>{{ text.ordersPer15m }}</span></div>
          <StatusBadge :label="inflowStatus.label" :tone="inflowStatus.tone" />
        </article>

        <article class="metric-card">
          <div class="card-topline">
            <p class="card-kicker">{{ text.congestion }}</p>
            <InfoHint :title="helpContent.congestion.title" :body="helpContent.congestion.body" :points="helpContent.congestion.points" align="right" />
          </div>
          <div class="metric-line"><strong>{{ formatNumber(selectedCongestion?.congestionScore, 1) }}</strong><span>{{ text.score }}</span></div>
          <StatusBadge :label="congestionStatus.label" :tone="congestionStatus.tone" />
        </article>
      </section>

      <section class="panel-grid">
        <article class="panel">
          <div class="panel-heading">
            <div class="title-cluster">
              <p class="card-kicker">{{ text.shippingDelayTrend }}</p>
              <div class="title-line">
                <h3>{{ text.delayBuildUp }}</h3>
                <InfoHint :title="helpContent.shippingDelay.title" :body="helpContent.shippingDelay.body" :points="locale === 'ko' ? ['라인은 선택한 스냅샷에서 멈춥니다.', '커서를 가까운 지점에 올리면 가장 인접한 값을 확인할 수 있습니다.'] : ['The line stops at the selected snapshot.', 'Hover near any point to inspect the closest observed value.']" />
              </div>
              <p class="panel-description">{{ text.delayTrendDescription }}</p>
            </div>
            <div class="panel-summary"><span>{{ text.current }}</span><strong>{{ formatMinutes(selectedShippingDelay?.avgDelayMinutesNext30m) }}</strong></div>
          </div>
          <TimeSeriesChart :labels="visibleSnapshotOptions" :display-labels="snapshotLabels" :selected-label="selectedSnapshot" :series="delaySeries" :value-formatter="formatMinutes" :tooltip-label-formatter="(value) => formatTimestamp(value, locale)" :empty-label="locale === 'ko' ? '이 시나리오에는 표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'" />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div class="title-cluster">
              <p class="card-kicker">{{ text.orderInflowWindows }}</p>
              <div class="title-line">
                <h3>{{ text.demandPressure }}</h3>
                <InfoHint :title="helpContent.orderInflow.title" :body="helpContent.orderInflow.body" :points="helpContent.orderInflow.points" />
              </div>
              <p class="panel-description">{{ text.inflowDescription }}</p>
            </div>
            <StatusBadge :label="inflowStatus.label" :tone="inflowStatus.tone" />
          </div>
          <div class="mini-stats">
            <div class="mini-stat"><span>15m</span><strong>{{ formatNumber(inflowCards.current15m, 1) }}</strong></div>
            <div class="mini-stat"><span>30m</span><strong>{{ formatNumber(inflowCards.current30m, 1) }}</strong></div>
            <div class="mini-stat"><span>1h</span><strong>{{ formatNumber(inflowCards.current60m, 1) }}</strong></div>
          </div>
          <TimeSeriesChart :labels="visibleSnapshotOptions" :display-labels="snapshotLabels" :selected-label="selectedSnapshot" :series="inflowSeries" :value-formatter="formatOrders" :tooltip-label-formatter="(value) => formatTimestamp(value, locale)" :empty-label="locale === 'ko' ? '이 시나리오에는 표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'" />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div class="title-cluster">
              <p class="card-kicker">{{ text.robotStateMix }}</p>
              <div class="title-line">
                <h3>{{ text.capacityPosture }}</h3>
                <InfoHint :title="helpContent.robot.title" :body="helpContent.robot.body" :points="helpContent.robot.points" />
              </div>
              <p class="panel-description">{{ text.robotDescription }}</p>
            </div>
            <div class="panel-summary"><span>{{ text.avgIdle }}</span><strong>{{ formatMinutes(selectedRobotSummary?.avgIdleDurationMin) }}</strong></div>
          </div>
          <div class="robot-layout">
            <RobotDonutChart :segments="robotSegments">
              <div class="donut-center"><strong>{{ formatNumber(robotTotals.total) }}</strong><span>{{ text.robotsLive }}</span></div>
            </RobotDonutChart>
            <div class="robot-legend">
              <div v-for="segment in robotSegments" :key="segment.key" class="robot-item">
                <span class="robot-swatch" :style="{ backgroundColor: segment.color }"></span>
                <div class="robot-copy">
                  <p>{{ segment.label }}</p>
                  <strong>{{ formatNumber(segment.value) }}</strong>
                  <span>{{ segment.share }}%</span>
                </div>
              </div>
            </div>
          </div>
          <TimeSeriesChart :labels="visibleSnapshotOptions" :display-labels="snapshotLabels" :selected-label="selectedSnapshot" :series="robotSeries" :value-formatter="formatRobots" :tooltip-label-formatter="(value) => formatTimestamp(value, locale)" :empty-label="locale === 'ko' ? '이 시나리오에는 표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'" />
        </article>

        <article class="panel">
          <div class="panel-heading">
            <div class="title-cluster">
              <p class="card-kicker">{{ text.congestionScore }}</p>
              <div class="title-line">
                <h3>{{ text.flowFriction }}</h3>
                <InfoHint :title="helpContent.congestion.title" :body="helpContent.congestion.body" :points="helpContent.congestion.points" align="right" />
              </div>
              <p class="panel-description">{{ text.congestionDescription }}</p>
            </div>
            <StatusBadge :label="congestionStatus.label" :tone="congestionStatus.tone" />
          </div>
          <TimeSeriesChart :labels="visibleSnapshotOptions" :display-labels="snapshotLabels" :selected-label="selectedSnapshot" :series="congestionSeries" :value-formatter="formatScore" :tooltip-label-formatter="(value) => formatTimestamp(value, locale)" :empty-label="locale === 'ko' ? '이 시나리오에는 표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'" />
        </article>
      </section>

      <section v-if="isFactoryLoading || isDashboardLoading" class="message-card">
        <p>{{ text.refreshingView }}</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.dashboard-page{min-height:100vh;padding:1.6rem;background:radial-gradient(circle at top left,rgba(249,115,22,.22),transparent 28%),radial-gradient(circle at 90% 0%,rgba(37,99,235,.16),transparent 30%),linear-gradient(180deg,#fff7ed 0%,#fffcf7 28%,#eef4ff 100%);color:#18212f;font-family:'Aptos','Trebuchet MS','Segoe UI',sans-serif}
.hero-shell,.context-card,.metric-card,.panel,.message-card,.hero-card{border:1px solid rgba(15,23,42,.08);background:rgba(255,255,255,.82);box-shadow:0 24px 60px rgba(15,23,42,.08);backdrop-filter:blur(16px)}
.hero-shell{display:grid;gap:1.1rem;padding:1.4rem;border-radius:2rem}
.eyebrow,.card-kicker{color:#c2410c;text-transform:uppercase;letter-spacing:.16em;font-size:.72rem;font-weight:800}
.hero-copy h1,.context-main h2,.panel-heading h3{font-family:'Bahnschrift','Aptos Display','Trebuchet MS',sans-serif}
.hero-topline{display:flex;justify-content:space-between;gap:1rem;align-items:center}
.hero-copy h1{margin-top:.55rem;font-size:clamp(2.4rem,5vw,4.6rem);line-height:.96;font-weight:900}
.hero-text{max-width:46rem;margin-top:.9rem;color:#475569;font-size:1rem;line-height:1.65}
.locale-toggle{display:inline-flex;padding:.22rem;border-radius:999px;background:rgba(255,255,255,.72);box-shadow:inset 0 0 0 1px rgba(15,23,42,.08)}
.locale-button{min-width:3rem;padding:.5rem .8rem;border:0;border-radius:999px;background:transparent;color:#64748b;font-weight:800;cursor:pointer}
.locale-button.is-active{background:#111827;color:#fff;box-shadow:0 10px 20px rgba(15,23,42,.18)}
.window-pill-row,.hero-stack,.control-grid,.overview-grid,.panel-grid{display:grid;gap:1rem}
.window-pill-row{grid-template-columns:repeat(auto-fit,minmax(12rem,1fr));margin-top:1rem}
.window-pill,.hero-card{padding:1rem 1.05rem;border-radius:1.35rem}
.window-pill{background:rgba(255,255,255,.68);border:1px solid rgba(255,255,255,.6)}
.window-pill span,.hero-card span,.panel-summary span{display:block;color:#64748b;font-size:.8rem;font-weight:700}
.window-pill strong,.hero-card-value{display:block;margin-top:.35rem;font-size:1.35rem;font-weight:900}
.hero-stack{grid-template-columns:repeat(2,minmax(0,1fr))}
.hero-card{display:grid;gap:.85rem}
.hero-card--accent{background:linear-gradient(145deg,rgba(29,78,216,.96),rgba(14,116,144,.92));color:#fff}
.hero-card--accent .card-kicker,.hero-card--accent span{color:rgba(255,255,255,.78)}
.hero-card-copy{color:#64748b;line-height:1.55}
.hero-card--accent .hero-card-copy{color:rgba(255,255,255,.8)}
.pulse-grid{display:grid;gap:.8rem;grid-template-columns:repeat(3,minmax(0,1fr))}
.pulse-grid strong{display:block;margin-top:.4rem;font-size:1.1rem;font-weight:900}
.progress-shell{overflow:hidden;width:100%;height:.55rem;border-radius:999px;background:rgba(226,232,240,.9)}
.progress-shell span{display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,#f97316,#2563eb)}
.control-grid{grid-template-columns:repeat(auto-fit,minmax(14rem,1fr))}
.field{display:grid;gap:.45rem}
.field-label,.card-topline,.title-line{display:flex;align-items:center;gap:.5rem}
.field-label{color:#475569;font-size:.86rem;font-weight:800}
.field select{min-height:3rem;padding:.78rem .95rem;border:1px solid rgba(15,23,42,.12);border-radius:1rem;background:rgba(255,255,255,.95);color:#0f172a;box-shadow:inset 0 1px 0 rgba(255,255,255,.65)}
.overview-grid{margin-top:1.25rem;grid-template-columns:repeat(4,minmax(0,1fr))}
.context-card{grid-column:span 4;display:grid;gap:.95rem;padding:1.2rem;border-radius:1.7rem}
.context-main{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start}
.context-label{color:#64748b;font-size:.84rem;font-weight:700}
.context-main h2{margin-top:.35rem;font-size:2rem;font-weight:900}
.context-subtitle,.metric-note,.panel-description,.context-meta{color:#64748b}
.context-meta{display:flex;flex-wrap:wrap;gap:.8rem 1rem;font-size:.9rem}
.metric-card{display:grid;gap:.8rem;padding:1.2rem;border-radius:1.45rem}
.metric-line{display:flex;align-items:baseline;gap:.45rem}
.metric-line strong{font-size:2.35rem;line-height:1;font-weight:900}
.metric-line span{color:#64748b;font-weight:700}
.metric-note,.panel-description{font-size:.9rem;line-height:1.55}
.panel-grid{margin-top:1.2rem;grid-template-columns:repeat(2,minmax(0,1fr))}
.panel{display:grid;gap:1rem;padding:1.2rem;border-radius:1.6rem}
.panel-heading{display:flex;justify-content:space-between;gap:1rem;align-items:flex-start}
.title-cluster{display:grid;gap:.35rem}
.panel-heading h3{font-size:1.35rem;font-weight:900}
.panel-summary{min-width:8.5rem;padding:.85rem .95rem;border-radius:1.1rem;background:rgba(248,250,252,.92)}
.panel-summary strong{display:block;margin-top:.4rem;font-size:1.05rem;font-weight:900}
.mini-stats{display:grid;gap:.8rem;grid-template-columns:repeat(3,minmax(0,1fr))}
.mini-stat{padding:.95rem 1rem;border-radius:1.15rem;background:rgba(255,247,237,.95)}
.mini-stat span{display:block;color:#9a3412;font-size:.78rem;font-weight:800;letter-spacing:.08em;text-transform:uppercase}
.mini-stat strong{display:block;margin-top:.35rem;font-size:1.55rem;font-weight:900}
.robot-layout{display:grid;gap:1.3rem;grid-template-columns:minmax(0,16rem) minmax(0,1fr);align-items:center}
.donut-center strong{display:block;font-size:1.95rem;line-height:1;font-weight:900}
.donut-center span{color:#64748b;font-size:.86rem;font-weight:700}
.robot-legend{display:grid;gap:.8rem}
.robot-item{display:flex;gap:.85rem;align-items:center;padding:.9rem 1rem;border-radius:1rem;background:rgba(248,250,252,.9)}
.robot-swatch{width:.95rem;height:.95rem;border-radius:999px;flex:0 0 auto}
.robot-copy p{color:#475569;font-size:.82rem;font-weight:700}
.robot-copy strong{display:block;margin-top:.12rem;font-size:1.1rem;font-weight:900}
.robot-copy span{color:#94a3b8;font-size:.78rem;font-weight:700}
.message-card{display:grid;gap:.65rem;margin-top:1.25rem;padding:1rem 1.2rem;border-radius:1.3rem}
.message-card.is-error{border-color:rgba(220,38,38,.18);background:rgba(254,242,242,.9)}
.retry-button{width:fit-content;min-height:2.7rem;padding:.6rem 1rem;border:0;border-radius:999px;background:#111827;color:#fff;font-weight:800;cursor:pointer}
@media (max-width:1100px){.overview-grid,.panel-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.context-card{grid-column:span 2}}
@media (max-width:900px){.hero-stack,.panel-grid,.mini-stats,.pulse-grid{grid-template-columns:1fr}.robot-layout{grid-template-columns:1fr}}
@media (max-width:720px){.dashboard-page{padding:1rem}.hero-shell,.context-card,.metric-card,.panel{padding:1rem}.overview-grid{grid-template-columns:1fr}.context-card{grid-column:auto}.hero-topline,.context-main,.panel-heading{flex-direction:column;align-items:flex-start}.panel-summary{min-width:0;width:100%}}
</style>
