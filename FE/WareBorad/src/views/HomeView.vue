<script setup>
import { computed, onMounted, ref } from 'vue'

import { getDashboard, getFactories, getFactoryInfo, getScenarios } from '@/api/dashboard'
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

const isBooting = ref(false)
const isFactoryLoading = ref(false)
const isDashboardLoading = ref(false)
const errorMessage = ref('')

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

const snapshotOptions = computed(() => {
  const timeline = [
    ...shippingDelay.value,
    ...orderInflow.value,
    ...robotSummary.value,
    ...congestion.value,
  ]

  const values = Array.from(
    new Set(
      timeline
        .map((entry) => entry.snapshotTime)
        .filter(Boolean),
    ),
  )

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

const snapshotLabels = computed(() => visibleSnapshotOptions.value.map((value) => formatTimeLabel(value)))

const visibleShippingDelay = computed(() =>
  filterRowsThroughSnapshot(shippingDelay.value, selectedSnapshot.value),
)

const visibleOrderInflow = computed(() =>
  filterRowsThroughSnapshot(orderInflow.value, selectedSnapshot.value),
)

const visibleRobotSummary = computed(() =>
  filterRowsThroughSnapshot(robotSummary.value, selectedSnapshot.value),
)

const visibleCongestion = computed(() =>
  filterRowsThroughSnapshot(congestion.value, selectedSnapshot.value),
)

const selectedShippingDelay = computed(() => pointAt(visibleShippingDelay.value, selectedSnapshot.value))
const selectedOrderInflow = computed(() => pointAt(visibleOrderInflow.value, selectedSnapshot.value))
const selectedRobotSummary = computed(() => pointAt(visibleRobotSummary.value, selectedSnapshot.value))
const selectedCongestion = computed(() => pointAt(visibleCongestion.value, selectedSnapshot.value))

const delayStatus = computed(() => getDelayStatus(selectedShippingDelay.value?.avgDelayMinutesNext30m))
const inflowStatus = computed(() => getInflowStatus(selectedOrderInflow.value?.orderInflow15m))

const maxCongestionScore = computed(() =>
  visibleCongestion.value.reduce(
    (maxValue, entry) => Math.max(maxValue, Number(entry.congestionScore ?? 0)),
    0,
  ),
)

const congestionStatus = computed(() => {
  const score = selectedCongestion.value?.congestionScore

  if (score == null) {
    return { label: 'No data', tone: 'muted' }
  }

  const tone = getCongestionTone(score, maxCongestionScore.value)

  return {
    label: `Score ${formatNumber(score, 1)}`,
    tone,
  }
})

const inflowCards = computed(() => {
  const snapshotTime = selectedSnapshot.value

  return {
    current15m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 1, snapshotTime),
    current30m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 2, snapshotTime),
    current60m: sumRecent(visibleOrderInflow.value, 'orderInflow15m', 4, snapshotTime),
  }
})

const robotTotals = computed(() => {
  const active = Number(selectedRobotSummary.value?.robotActive ?? 0)
  const idle = Number(selectedRobotSummary.value?.robotIdle ?? 0)
  const charging = Number(selectedRobotSummary.value?.robotCharging ?? 0)
  const total = active + idle + charging

  return { active, idle, charging, total }
})

const robotSegments = computed(() => {
  const { active, idle, charging, total } = robotTotals.value

  if (!total) {
    return []
  }

  return [
    {
      key: 'active',
      label: 'Active',
      value: active,
      angle: (active / total) * 360,
      color: '#f97316',
    },
    {
      key: 'idle',
      label: 'Idle',
      value: idle,
      angle: (idle / total) * 360,
      color: '#0f766e',
    },
    {
      key: 'charging',
      label: 'Charging',
      value: charging,
      angle: (charging / total) * 360,
      color: '#1d4ed8',
    },
  ]
})

const delaySeries = computed(() => [
  {
    key: 'delay',
    label: 'Delay',
    color: '#dc2626',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleShippingDelay.value, snapshotTime, 'avgDelayMinutesNext30m'),
    ),
  },
])

const inflowSeries = computed(() => [
  {
    key: 'inflow',
    label: 'Order inflow',
    color: '#ea580c',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleOrderInflow.value, snapshotTime, 'orderInflow15m'),
    ),
  },
])

const robotSeries = computed(() => [
  {
    key: 'active',
    label: 'Active',
    color: '#f97316',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleRobotSummary.value, snapshotTime, 'robotActive'),
    ),
  },
  {
    key: 'idle',
    label: 'Idle',
    color: '#0f766e',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleRobotSummary.value, snapshotTime, 'robotIdle'),
    ),
  },
  {
    key: 'charging',
    label: 'Charging',
    color: '#1d4ed8',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleRobotSummary.value, snapshotTime, 'robotCharging'),
    ),
  },
])

const congestionSeries = computed(() => [
  {
    key: 'congestion',
    label: 'Congestion',
    color: '#7c3aed',
    values: visibleSnapshotOptions.value.map((snapshotTime) =>
      metricAt(visibleCongestion.value, snapshotTime, 'congestionScore'),
    ),
  },
])

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
    errorMessage.value = error.message || 'Failed to load dashboard data.'
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
    errorMessage.value = error.message || 'Failed to load factory data.'
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
    errorMessage.value = error.message || 'Failed to load factories.'
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
  <main class="dashboard-view">
    <section class="hero-panel">
      <div class="hero-copy">
        <p class="eyebrow">Smart Warehouse Outbound Monitor</p>
        <h1>출고 지연 대시보드</h1>
        <p class="hero-text">
          공장, 시나리오, 스냅샷 시점을 기준으로 출고 지연과 주문 유입, 로봇 상태, 혼잡도를
          한 화면에서 확인할 수 있습니다.
        </p>
      </div>

      <div class="control-grid">
        <label class="field">
          <span>Factory</span>
          <select :value="selectedFactoryId" :disabled="isBooting || isFactoryLoading" @change="handleFactoryChange">
            <option v-for="factory in factories" :key="factory.layoutId" :value="factory.layoutId">
              {{ factory.layoutId }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>Scenario</span>
          <select
            :value="selectedScenarioId"
            :disabled="isBooting || isFactoryLoading || !scenarios.length"
            @change="handleScenarioChange"
          >
            <option v-for="scenario in scenarios" :key="scenario.scenarioId" :value="scenario.scenarioId">
              {{ scenario.scenarioId }}
            </option>
          </select>
        </label>

        <label class="field">
          <span>Snapshot</span>
          <select v-model="selectedSnapshot" :disabled="isDashboardLoading || !snapshotOptions.length">
            <option v-for="snapshot in snapshotOptions" :key="snapshot" :value="snapshot">
              {{ formatTimestamp(snapshot) }}
            </option>
          </select>
        </label>
      </div>
    </section>

    <section v-if="errorMessage" class="message-card is-error">
      <strong>데이터를 불러오지 못했습니다.</strong>
      <p>{{ errorMessage }}</p>
      <button class="retry-button" type="button" @click="bootstrapDashboard">다시 시도</button>
    </section>

    <section v-else-if="isBooting" class="message-card">
      <p>대시보드를 준비하고 있습니다...</p>
    </section>

    <template v-else>
      <section class="summary-grid">
        <article class="summary-card summary-card--wide">
          <p class="card-kicker">Current context</p>
          <div class="summary-headline">
            <div>
              <h2>{{ factoryInfo?.layoutId ?? '-' }}</h2>
              <p>{{ selectedScenarioId || 'No scenario selected' }}</p>
            </div>
            <StatusBadge :label="delayStatus.label" :tone="delayStatus.tone" />
          </div>
          <div class="meta-strip">
            <span>Layout type {{ factoryInfo?.layoutType ?? '-' }}</span>
            <span>Floor area {{ formatNumber(factoryInfo?.floorAreaSqm) }} sqm</span>
            <span>Total robots {{ formatNumber(factoryInfo?.robotTotal) }}</span>
            <span>Snapshot {{ formatTimestamp(selectedSnapshot) }}</span>
          </div>
        </article>

        <article class="summary-card">
          <p class="card-kicker">Shipping delay</p>
          <div class="metric-line">
            <strong>{{ formatNumber(selectedShippingDelay?.avgDelayMinutesNext30m, 1) }}</strong>
            <span>min</span>
          </div>
          <p class="helper-text">예상 출고 지연 시간</p>
        </article>

        <article class="summary-card">
          <p class="card-kicker">Order inflow</p>
          <div class="metric-line">
            <strong>{{ formatNumber(selectedOrderInflow?.orderInflow15m, 1) }}</strong>
            <span>orders / 15m</span>
          </div>
          <StatusBadge :label="inflowStatus.label" :tone="inflowStatus.tone" />
        </article>

        <article class="summary-card">
          <p class="card-kicker">Congestion</p>
          <div class="metric-line">
            <strong>{{ formatNumber(selectedCongestion?.congestionScore, 1) }}</strong>
            <span>score</span>
          </div>
          <StatusBadge :label="congestionStatus.label" :tone="congestionStatus.tone" />
        </article>
      </section>

      <section class="panel-grid">
        <article class="panel">
          <div class="panel-header">
            <div>
              <p class="card-kicker">Shipping delay trend</p>
              <h3>출고 지연 추이</h3>
            </div>
            <p class="panel-value">{{ formatNumber(selectedShippingDelay?.avgDelayMinutesNext30m, 1) }} min</p>
          </div>
          <TimeSeriesChart
            :labels="visibleSnapshotOptions"
            :display-labels="snapshotLabels"
            :selected-label="selectedSnapshot"
            :series="delaySeries"
          />
        </article>

        <article class="panel">
          <div class="panel-header">
            <div>
              <p class="card-kicker">Order inflow windows</p>
              <h3>주문 유입량</h3>
            </div>
            <StatusBadge :label="inflowStatus.label" :tone="inflowStatus.tone" />
          </div>

          <div class="mini-stats">
            <div class="mini-stat">
              <span>15m</span>
              <strong>{{ formatNumber(inflowCards.current15m, 1) }}</strong>
            </div>
            <div class="mini-stat">
              <span>30m</span>
              <strong>{{ formatNumber(inflowCards.current30m, 1) }}</strong>
            </div>
            <div class="mini-stat">
              <span>1h</span>
              <strong>{{ formatNumber(inflowCards.current60m, 1) }}</strong>
            </div>
          </div>

          <TimeSeriesChart
            :labels="visibleSnapshotOptions"
            :display-labels="snapshotLabels"
            :selected-label="selectedSnapshot"
            :series="inflowSeries"
          />
        </article>

        <article class="panel">
          <div class="panel-header">
            <div>
              <p class="card-kicker">Robot state mix</p>
              <h3>로봇 운영 현황</h3>
            </div>
            <p class="panel-value">{{ formatNumber(selectedRobotSummary?.avgIdleDurationMin, 1) }} min idle</p>
          </div>

          <div class="robot-layout">
            <RobotDonutChart :segments="robotSegments">
              <div class="donut-center">
                <strong>{{ formatNumber(robotTotals.total) }}</strong>
                <span>robots</span>
              </div>
            </RobotDonutChart>

            <div class="robot-legend">
              <div v-for="segment in robotSegments" :key="segment.key" class="robot-item">
                <span class="robot-swatch" :style="{ backgroundColor: segment.color }"></span>
                <div>
                  <p>{{ segment.label }}</p>
                  <strong>{{ formatNumber(segment.value) }}</strong>
                </div>
              </div>
            </div>
          </div>

          <TimeSeriesChart
            :labels="visibleSnapshotOptions"
            :display-labels="snapshotLabels"
            :selected-label="selectedSnapshot"
            :series="robotSeries"
          />
        </article>

        <article class="panel">
          <div class="panel-header">
            <div>
              <p class="card-kicker">Congestion score</p>
              <h3>혼잡도 변화</h3>
            </div>
            <StatusBadge :label="congestionStatus.label" :tone="congestionStatus.tone" />
          </div>
          <TimeSeriesChart
            :labels="visibleSnapshotOptions"
            :display-labels="snapshotLabels"
            :selected-label="selectedSnapshot"
            :series="congestionSeries"
          />
        </article>
      </section>

      <section v-if="isFactoryLoading || isDashboardLoading" class="message-card">
        <p>최신 스냅샷을 불러오는 중입니다...</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.dashboard-view {
  min-height: 100vh;
  padding: 2rem;
  background:
    radial-gradient(circle at top left, rgba(251, 146, 60, 0.22), transparent 32%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.18), transparent 28%),
    linear-gradient(180deg, #fff8f1 0%, #f7f7f3 42%, #eef4ff 100%);
  color: #18212f;
}

.hero-panel,
.summary-card,
.panel,
.message-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.hero-panel {
  display: grid;
  gap: 1.5rem;
  padding: 1.6rem;
  border-radius: 1.8rem;
}

.eyebrow,
.card-kicker {
  color: #c2410c;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.72rem;
  font-weight: 800;
}

.hero-copy h1 {
  margin-top: 0.55rem;
  font-size: clamp(2rem, 4vw, 3.8rem);
  line-height: 1;
  font-weight: 900;
}

.hero-text {
  max-width: 52rem;
  margin-top: 0.8rem;
  color: #475569;
  font-size: 1rem;
}

.control-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
}

.field {
  display: grid;
  gap: 0.45rem;
  font-size: 0.88rem;
  font-weight: 700;
  color: #475569;
}

.field select {
  min-height: 3rem;
  padding: 0.75rem 0.9rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.95);
  color: #0f172a;
}

.summary-grid,
.panel-grid {
  display: grid;
  gap: 1.2rem;
  margin-top: 1.25rem;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card {
  display: grid;
  gap: 0.75rem;
  padding: 1.2rem;
  border-radius: 1.5rem;
}

.summary-card--wide {
  grid-column: span 4;
}

.summary-headline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.summary-headline h2 {
  font-size: 1.75rem;
  font-weight: 800;
}

.summary-headline p,
.helper-text,
.meta-strip {
  color: #64748b;
}

.metric-line {
  display: flex;
  align-items: baseline;
  gap: 0.45rem;
}

.metric-line strong {
  font-size: 2.1rem;
  line-height: 1;
  font-weight: 900;
}

.metric-line span {
  color: #64748b;
  font-weight: 700;
}

.meta-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1.1rem;
  font-size: 0.92rem;
}

.panel-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  display: grid;
  gap: 1rem;
  padding: 1.2rem;
  border-radius: 1.5rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.panel-header h3 {
  margin-top: 0.35rem;
  font-size: 1.2rem;
  font-weight: 800;
}

.panel-value {
  color: #0f172a;
  font-size: 0.95rem;
  font-weight: 700;
}

.mini-stats {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mini-stat {
  padding: 0.9rem 1rem;
  border-radius: 1.1rem;
  background: rgba(255, 247, 237, 0.95);
}

.mini-stat span {
  display: block;
  color: #9a3412;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.mini-stat strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.55rem;
  font-weight: 900;
}

.robot-layout {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: minmax(0, 16rem) minmax(0, 1fr);
  align-items: center;
}

.donut-center strong {
  display: block;
  font-size: 1.8rem;
  line-height: 1;
  font-weight: 900;
}

.donut-center span {
  color: #64748b;
  font-size: 0.86rem;
  font-weight: 700;
}

.robot-legend {
  display: grid;
  gap: 0.8rem;
}

.robot-item {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: rgba(248, 250, 252, 0.9);
}

.robot-swatch {
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 999px;
  flex: 0 0 auto;
}

.robot-item p {
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
}

.robot-item strong {
  font-size: 1.1rem;
  font-weight: 900;
}

.message-card {
  display: grid;
  gap: 0.65rem;
  margin-top: 1.25rem;
  padding: 1rem 1.2rem;
  border-radius: 1.3rem;
}

.message-card.is-error {
  border-color: rgba(220, 38, 38, 0.18);
  background: rgba(254, 242, 242, 0.9);
}

.retry-button {
  width: fit-content;
  min-height: 2.7rem;
  padding: 0.6rem 1rem;
  border: 0;
  border-radius: 999px;
  background: #111827;
  color: white;
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 960px) {
  .summary-grid,
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .summary-card--wide {
    grid-column: auto;
  }

  .robot-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-view {
    padding: 1rem;
  }

  .hero-panel,
  .summary-card,
  .panel {
    padding: 1rem;
  }

  .summary-headline,
  .panel-header {
    flex-direction: column;
  }

  .mini-stats {
    grid-template-columns: 1fr;
  }
}
</style>
