<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import DonutChart from '@/components/DonutChart.vue'
import InfoHint from '@/components/InfoHint.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const dashboard = useDashboardStore()

const toneColors = {
  good: '#0f766e',
  watch: '#f59e0b',
  warning: '#fb923c',
  critical: '#ef4444',
  muted: '#94a3b8',
}

function buildToneSegments(tones) {
  const total = tones.reduce((sum, entry) => sum + entry.value, 0)
  if (!total) return []

  return tones
    .filter((entry) => entry.value > 0)
    .map((entry) => ({
      ...entry,
      share: Math.round((entry.value / total) * 100),
      angle: (entry.value / total) * 360,
      color: toneColors[entry.key] ?? toneColors.muted,
    }))
}

const delayDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of dashboard.visibleShippingDelay) {
    const status = dashboard.getDelayStatus(row.avgDelayMinutesNext30m, dashboard.locale)
    counts[status.tone] = (counts[status.tone] ?? 0) + 1
  }

  return buildToneSegments([
    { key: 'good', label: 'Good', value: counts.good },
    { key: 'watch', label: 'Watch', value: counts.watch },
    { key: 'warning', label: 'Warning', value: counts.warning },
    { key: 'critical', label: 'Critical', value: counts.critical },
    { key: 'muted', label: 'No data', value: counts.muted },
  ])
})

const inflowDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of dashboard.visibleOrderInflow) {
    const status = dashboard.getInflowStatus(row.orderInflow15m, dashboard.locale)
    counts[status.tone] = (counts[status.tone] ?? 0) + 1
  }

  return buildToneSegments([
    { key: 'good', label: 'Good', value: counts.good },
    { key: 'watch', label: 'Watch', value: counts.watch },
    { key: 'warning', label: 'Warning', value: counts.warning },
    { key: 'critical', label: 'Critical', value: counts.critical },
    { key: 'muted', label: 'No data', value: counts.muted },
  ])
})

const congestionDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of dashboard.visibleCongestion) {
    const tone = dashboard.getCongestionTone(row.congestionScore, dashboard.maxCongestionScore)
    counts[tone] = (counts[tone] ?? 0) + 1
  }

  return buildToneSegments([
    { key: 'good', label: 'Good', value: counts.good },
    { key: 'watch', label: 'Watch', value: counts.watch },
    { key: 'warning', label: 'Warning', value: counts.warning },
    { key: 'critical', label: 'Critical', value: counts.critical },
    { key: 'muted', label: 'No data', value: counts.muted },
  ])
})

const alertFeed = computed(() => {
  const items = []

  const delayTone = dashboard.delayStatus.tone
  if (delayTone !== 'good' && delayTone !== 'muted') {
    items.push({
      key: 'delay',
      title: dashboard.text.shippingDelay,
      tone: delayTone,
      message: dashboard.locale === 'ko'
        ? '출고 지연 위험이 증가했습니다. 추이와 누적 흐름을 확인하세요.'
        : 'Shipping delay risk is elevated. Check the trend and cumulative flow.',
    })
  }

  const inflowTone = dashboard.inflowStatus.tone
  if (inflowTone === 'warning' || inflowTone === 'critical') {
    items.push({
      key: 'inflow',
      title: dashboard.text.demandPressure,
      tone: inflowTone,
      message: dashboard.locale === 'ko'
        ? '유입량이 높습니다. 수요 압력 탭에서 구간 합계를 확인하세요.'
        : 'Inflow is high. Inspect demand windows in the Demand pressure tab.',
    })
  }

  const congestionTone = dashboard.congestionStatus.tone
  if (congestionTone === 'warning' || congestionTone === 'critical') {
    items.push({
      key: 'congestion',
      title: dashboard.text.flowFriction,
      tone: congestionTone,
      message: dashboard.locale === 'ko'
        ? '혼잡도가 높습니다. 흐름 마찰도 원인(유입/로봇 상태)을 함께 비교해보세요.'
        : 'Congestion is high. Compare inflow and robot posture to find the driver.',
    })
  }

  const capacityTone = dashboard.capacityStatus.tone
  if (capacityTone === 'warning' || capacityTone === 'critical') {
    items.push({
      key: 'capacity',
      title: dashboard.text.capacity,
      tone: capacityTone,
      message: dashboard.locale === 'ko'
        ? '가동 여력이 낮습니다. 로봇 상태 믹스와 평균 유휴 시간을 확인하세요.'
        : 'Capacity is tight. Review the robot state mix and average idle duration.',
    })
  }

  return items
})

const summaryTableRows = computed(() => {
  const snapshots = dashboard.visibleSnapshotOptions
  if (!snapshots.length) return []

  const last = snapshots.slice(Math.max(0, snapshots.length - 12)).reverse()
  return last.map((snapshotTime) => {
    const delay = dashboard.metricAt(dashboard.visibleShippingDelay, snapshotTime, 'avgDelayMinutesNext30m')
    const inflow = dashboard.metricAt(dashboard.visibleOrderInflow, snapshotTime, 'orderInflow15m')
    const active = dashboard.metricAt(dashboard.visibleRobotSummary, snapshotTime, 'robotActive')
    const congestion = dashboard.metricAt(dashboard.visibleCongestion, snapshotTime, 'congestionScore')

    return {
      snapshotTime,
      delay,
      inflow,
      active,
      congestion,
      delayTone: dashboard.getDelayStatus(delay, dashboard.locale).tone,
      inflowTone: dashboard.getInflowStatus(inflow, dashboard.locale).tone,
      congestionTone: dashboard.getCongestionTone(congestion, dashboard.maxCongestionScore),
    }
  })
})

function toneClass(tone) {
  if (!tone) return 'is-muted'
  return `is-${tone}`
}
</script>

<template>
  <section class="page">
    <header class="hero">
      <p class="hero-kicker">{{ dashboard.text.appTagline }}</p>
      <h1>{{ dashboard.text.summaryTitle }}</h1>
      <p class="hero-subtitle">{{ dashboard.text.summarySubtitle }}</p>
    </header>

    <section v-if="dashboard.errorMessage" class="message-card is-error">
      <strong>{{ dashboard.locale === 'ko' ? '대시보드 데이터를 불러오지 못했습니다.' : 'Dashboard data could not be loaded.' }}</strong>
      <p>{{ dashboard.errorMessage }}</p>
      <button class="retry-button" type="button" @click="dashboard.bootstrap">{{ dashboard.text.reloadDashboard }}</button>
    </section>

    <section v-else-if="dashboard.isBooting" class="message-card">
      <p>{{ dashboard.text.preparingDashboard }}</p>
    </section>

    <template v-else>
      <section class="grid context-grid">
        <article class="card context-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.currentContext }}</p>
            <InfoHint :title="dashboard.helpContent.window.title" :body="dashboard.helpContent.window.body" :points="dashboard.helpContent.window.points" align="right" />
          </div>
          <div class="context-main">
            <div>
              <p class="context-label">{{ dashboard.factoryInfo?.layoutType ?? dashboard.text.warehouseLayout }}</p>
              <h2 class="context-title">{{ dashboard.factoryInfo?.layoutId ?? '-' }}</h2>
              <p class="context-subtitle">{{ dashboard.selectedScenarioId || dashboard.text.noScenarioSelected }}</p>
            </div>
            <div class="status-stack">
              <StatusBadge :label="dashboard.delayStatus.label" :tone="dashboard.delayStatus.tone" />
              <StatusBadge :label="dashboard.inflowStatus.label" :tone="dashboard.inflowStatus.tone" />
              <StatusBadge :label="dashboard.capacityStatus.label" :tone="dashboard.capacityStatus.tone" />
              <StatusBadge :label="dashboard.congestionStatus.label" :tone="dashboard.congestionStatus.tone" />
            </div>
          </div>
          <div class="context-meta">
            <span>{{ dashboard.text.floorArea }} {{ dashboard.formatNumber(dashboard.factoryInfo?.floorAreaSqm) }} sqm</span>
            <span>{{ dashboard.text.totalRobots }} {{ dashboard.formatNumber(dashboard.factoryInfo?.robotTotal) }}</span>
            <span>{{ dashboard.text.snapshot }} {{ dashboard.formatTimestamp(dashboard.selectedSnapshot, dashboard.locale) }}</span>
            <span>{{ dashboard.text.visibleWindow }} {{ dashboard.timeWindowLabel }}</span>
          </div>
        </article>

        <article class="card pulse-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.locale === 'ko' ? '운영 요약' : 'Operational summary' }}</p>
          </div>
          <div class="pulse-grid">
            <div class="pulse-item">
              <span>{{ dashboard.locale === 'ko' ? '평균 지연' : 'Avg delay' }}</span>
              <strong>{{ dashboard.formatMinutes(dashboard.averageDelaySoFar) }}</strong>
            </div>
            <div class="pulse-item">
              <span>{{ dashboard.locale === 'ko' ? '최대 유입(15m)' : 'Peak inflow (15m)' }}</span>
              <strong>{{ dashboard.formatOrders(dashboard.peakInflowSoFar) }}</strong>
            </div>
            <div class="pulse-item">
              <span>{{ dashboard.locale === 'ko' ? '평균 혼잡도' : 'Avg congestion' }}</span>
              <strong>{{ dashboard.formatScore(dashboard.averageCongestionSoFar) }}</strong>
            </div>
          </div>
          <div class="pulse-note">
            <p>{{ dashboard.locale === 'ko' ? '모든 수치는 선택한 스냅샷 시점까지 들어온 값만 계산합니다.' : 'All stats are computed only from data up to the selected snapshot.' }}</p>
          </div>
        </article>
      </section>

      <section class="grid kpi-grid">
        <article class="card kpi-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.shippingDelay }}</p>
          </div>
          <div class="kpi-line">
            <strong>{{ dashboard.formatNumber(dashboard.selectedShippingDelay?.avgDelayMinutesNext30m, 1) }}</strong>
            <span>{{ dashboard.locale === 'ko' ? '분' : 'min' }}</span>
          </div>
          <p class="kpi-note">{{ dashboard.text.shippingDelayNote }}</p>
          <StatusBadge :label="dashboard.delayStatus.label" :tone="dashboard.delayStatus.tone" />
        </article>

        <article class="card kpi-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.demandPressure }}</p>
          </div>
          <div class="kpi-line">
            <strong>{{ dashboard.formatNumber(dashboard.selectedOrderInflow?.orderInflow15m, 1) }}</strong>
            <span>{{ dashboard.text.ordersPer15m }}</span>
          </div>
          <p class="kpi-note">
            {{ dashboard.locale === 'ko' ? '30분, 1시간 집계는 선택한 스냅샷까지 들어온 주문만 합산합니다.' : 'Rolling sums stop at the selected snapshot so they reflect only arrived demand.' }}
          </p>
          <StatusBadge :label="dashboard.inflowStatus.label" :tone="dashboard.inflowStatus.tone" />
        </article>

        <article class="card kpi-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.capacity }}</p>
          </div>
          <div class="kpi-line">
            <strong>{{ dashboard.formatNumber(dashboard.robotTotals.total) }}</strong>
            <span>{{ dashboard.text.robotsLive }}</span>
          </div>
          <p class="kpi-note">
            {{ dashboard.locale === 'ko' ? '유휴·충전 비율이 높을수록 여력이 큽니다.' : 'Higher idle/charging share generally means more spare capacity.' }}
          </p>
          <StatusBadge :label="dashboard.capacityStatus.label" :tone="dashboard.capacityStatus.tone" />
        </article>

        <article class="card kpi-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.flowFriction }}</p>
          </div>
          <div class="kpi-line">
            <strong>{{ dashboard.formatNumber(dashboard.selectedCongestion?.congestionScore, 1) }}</strong>
            <span>{{ dashboard.text.score }}</span>
          </div>
          <p class="kpi-note">
            {{ dashboard.locale === 'ko' ? '혼잡도 점수는 표시 구간 내 최고값 대비 상대 비교로 색이 바뀝니다.' : 'Congestion color is relative to the highest score observed in the visible window.' }}
          </p>
          <StatusBadge :label="dashboard.congestionStatus.label" :tone="dashboard.congestionStatus.tone" />
        </article>
      </section>

      <section class="grid lower-grid">
        <article class="card dist-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.distribution }}</p>
          </div>
          <div class="dist-grid">
            <div class="dist-item">
              <DonutChart :segments="delayDistribution" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.shippingDelay }}</strong>
                  <span>{{ dashboard.timeWindowLabel }}</span>
                </div>
              </DonutChart>
            </div>
            <div class="dist-item">
              <DonutChart :segments="inflowDistribution" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.demandPressure }}</strong>
                  <span>{{ dashboard.timeWindowLabel }}</span>
                </div>
              </DonutChart>
            </div>
            <div class="dist-item">
              <DonutChart :segments="congestionDistribution" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.flowFriction }}</strong>
                  <span>{{ dashboard.timeWindowLabel }}</span>
                </div>
              </DonutChart>
            </div>
          </div>
          <p class="dist-note">
            {{ dashboard.locale === 'ko' ? '분포는 선택 스냅샷까지의 상태 톤(좋음/주의/경고/위험)을 집계한 결과입니다.' : 'Distributions count status tones (good/watch/warning/critical) up to the selected snapshot.' }}
          </p>
        </article>

        <article class="card alert-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.alertFeed }}</p>
          </div>

          <div v-if="!alertFeed.length" class="empty-alert">
            <p>{{ dashboard.locale === 'ko' ? '현재 시점에는 뚜렷한 경고가 없습니다.' : 'No notable alerts at the selected snapshot.' }}</p>
          </div>

          <ul v-else class="alert-list">
            <li v-for="alert in alertFeed" :key="alert.key" class="alert-item" :class="toneClass(alert.tone)">
              <div class="alert-title">
                <strong>{{ alert.title }}</strong>
                <span class="alert-tone">{{ alert.tone }}</span>
              </div>
              <p class="alert-message">{{ alert.message }}</p>
            </li>
          </ul>
        </article>
      </section>

      <section class="card table-card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.text.recentLog }}</p>
          <InfoHint :title="dashboard.helpContent.logs.title" :body="dashboard.helpContent.logs.body" :points="dashboard.helpContent.logs.points" align="right" />
        </div>

        <div class="table-shell">
          <table>
            <thead>
              <tr>
                <th>{{ dashboard.text.timestamp }}</th>
                <th>{{ dashboard.text.shippingDelay }}</th>
                <th>{{ dashboard.text.orderInflow }}</th>
                <th>{{ dashboard.locale === 'ko' ? '가동(로봇)' : 'Active (robots)' }}</th>
                <th>{{ dashboard.text.flowFriction }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in summaryTableRows" :key="row.snapshotTime">
                <td class="cell-time">{{ dashboard.formatTimestamp(row.snapshotTime, dashboard.locale) }}</td>
                <td>
                  <span class="tone-dot" :class="toneClass(row.delayTone)" aria-hidden="true"></span>
                  {{ dashboard.formatNumber(row.delay, 1) }}
                </td>
                <td>
                  <span class="tone-dot" :class="toneClass(row.inflowTone)" aria-hidden="true"></span>
                  {{ dashboard.formatNumber(row.inflow, 1) }}
                </td>
                <td>{{ dashboard.formatNumber(row.active) }}</td>
                <td>
                  <span class="tone-dot" :class="toneClass(row.congestionTone)" aria-hidden="true"></span>
                  {{ dashboard.formatNumber(row.congestion, 1) }}
                </td>
              </tr>
              <tr v-if="!summaryTableRows.length">
                <td class="empty-row" colspan="5">{{ dashboard.text.noData }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 1.25rem;
}

.hero {
  display: grid;
  gap: 0.55rem;
  padding: 1.25rem 1.4rem;
  border-radius: 2rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.hero-kicker {
  color: #c2410c;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.hero h1 {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: clamp(2.1rem, 4.5vw, 3.5rem);
  line-height: 0.98;
  font-weight: 900;
  color: #0f172a;
}

.hero-subtitle {
  max-width: 58rem;
  color: #475569;
  line-height: 1.6;
}

.message-card {
  padding: 1.2rem 1.25rem;
  border-radius: 1.6rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
  display: grid;
  gap: 0.65rem;
}

.message-card.is-error {
  border-color: rgba(239, 68, 68, 0.22);
  background: rgba(254, 242, 242, 0.92);
}

.retry-button {
  justify-self: start;
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1rem;
  background: #0f172a;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

.grid {
  display: grid;
  gap: 1rem;
}

.context-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.kpi-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.lower-grid {
  grid-template-columns: 2fr 1fr;
  align-items: start;
}

.card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
  border-radius: 1.7rem;
  padding: 1.15rem 1.2rem;
  display: grid;
  gap: 0.9rem;
}

.context-card {
  grid-column: span 2;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.card-kicker {
  color: #c2410c;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.context-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.context-label {
  color: #64748b;
  font-weight: 800;
}

.context-title {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 2.3rem;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.context-subtitle {
  color: #475569;
  margin-top: 0.25rem;
  font-weight: 700;
}

.status-stack {
  display: grid;
  gap: 0.45rem;
  justify-items: end;
}

.context-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1rem;
  color: #64748b;
  font-size: 0.9rem;
}

.pulse-card {
  background: linear-gradient(145deg, rgba(29, 78, 216, 0.92), rgba(14, 116, 144, 0.88));
  color: #fff;
  border-color: rgba(255, 255, 255, 0.2);
}

.pulse-card .card-kicker {
  color: rgba(255, 255, 255, 0.8);
}

.pulse-grid {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.pulse-item span {
  display: block;
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.8rem;
  font-weight: 800;
}

.pulse-item strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.15rem;
  font-weight: 900;
}

.pulse-note {
  padding: 0.75rem 0.9rem;
  border-radius: 1.2rem;
  background: rgba(15, 23, 42, 0.18);
  color: rgba(255, 255, 255, 0.82);
  font-size: 0.9rem;
}

.kpi-card {
  border-radius: 1.45rem;
}

.kpi-line {
  display: inline-flex;
  align-items: baseline;
  gap: 0.45rem;
}

.kpi-line strong {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 2.35rem;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.kpi-line span {
  color: #64748b;
  font-weight: 800;
}

.kpi-note {
  color: #64748b;
  font-size: 0.92rem;
  line-height: 1.55;
}

.dist-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
}

.donut-center strong {
  display: block;
  font-weight: 900;
  font-size: 0.85rem;
  color: #0f172a;
}

.donut-center span {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.72rem;
  color: #64748b;
  font-weight: 800;
}

.dist-note {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.55;
}

.alert-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.alert-item {
  border-radius: 1.2rem;
  padding: 0.85rem 0.9rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.9);
}

.alert-title {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: baseline;
}

.alert-title strong {
  font-weight: 900;
  color: #0f172a;
}

.alert-tone {
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #64748b;
}

.alert-message {
  margin-top: 0.35rem;
  color: #475569;
  font-size: 0.9rem;
  line-height: 1.55;
}

.empty-alert {
  color: #64748b;
  font-size: 0.95rem;
  line-height: 1.6;
  padding: 0.9rem 0.95rem;
  border-radius: 1.2rem;
  background: rgba(248, 250, 252, 0.9);
  border: 1px dashed rgba(15, 23, 42, 0.14);
}

.table-card {
  padding: 1.15rem 1.2rem 0.6rem;
}

.table-shell {
  overflow: auto;
  border-radius: 1.25rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 46rem;
}

thead th {
  text-align: left;
  padding: 0.85rem 0.9rem;
  color: #475569;
  font-weight: 900;
  font-size: 0.82rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  background: rgba(248, 250, 252, 0.92);
  position: sticky;
  top: 0;
  z-index: 1;
}

tbody td {
  padding: 0.82rem 0.9rem;
  border-top: 1px solid rgba(15, 23, 42, 0.06);
  color: #0f172a;
  font-weight: 700;
}

tbody tr:hover td {
  background: rgba(241, 245, 249, 0.72);
}

.cell-time {
  color: #475569;
  font-weight: 800;
  white-space: nowrap;
}

.tone-dot {
  display: inline-block;
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
  margin-right: 0.5rem;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.05);
  vertical-align: middle;
}

.tone-dot.is-good {
  background: #14b8a6;
}

.tone-dot.is-watch {
  background: #f59e0b;
}

.tone-dot.is-warning {
  background: #fb923c;
}

.tone-dot.is-critical {
  background: #ef4444;
}

.tone-dot.is-muted {
  background: #94a3b8;
}

.alert-item.is-warning {
  border-color: rgba(251, 146, 60, 0.32);
}

.alert-item.is-critical {
  border-color: rgba(239, 68, 68, 0.32);
}

.empty-row {
  color: #64748b;
  text-align: center;
  font-weight: 800;
}

@media (max-width: 1200px) {
  .context-grid {
    grid-template-columns: 1fr;
  }

  .context-card {
    grid-column: auto;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .lower-grid {
    grid-template-columns: 1fr;
  }

  .pulse-grid,
  .dist-grid {
    grid-template-columns: 1fr;
  }
}
</style>

