<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import DonutChart from '@/components/DonutChart.vue'
import InfoHint from '@/components/InfoHint.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TimeSeriesChart from '@/components/TimeSeriesChart.vue'

const dashboard = useDashboardStore()

const toneColors = {
  good: '#0f766e',
  watch: '#f59e0b',
  warning: '#fb923c',
  critical: '#ef4444',
  muted: '#94a3b8',
}

const inflowDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of dashboard.visibleOrderInflow) {
    const status = dashboard.getInflowStatus(row.orderInflow15m, dashboard.locale)
    counts[status.tone] = (counts[status.tone] ?? 0) + 1
  }

  const total = Object.values(counts).reduce((sum, value) => sum + value, 0)
  if (!total) return []

  return [
    { key: 'good', label: 'Good', value: counts.good },
    { key: 'watch', label: 'Watch', value: counts.watch },
    { key: 'warning', label: 'Warning', value: counts.warning },
    { key: 'critical', label: 'Critical', value: counts.critical },
    { key: 'muted', label: 'No data', value: counts.muted },
  ]
    .filter((entry) => entry.value > 0)
    .map((entry) => ({
      ...entry,
      share: Math.round((entry.value / total) * 100),
      angle: (entry.value / total) * 360,
      color: toneColors[entry.key] ?? toneColors.muted,
    }))
})

const inflowLog = computed(() =>
  [...dashboard.visibleOrderInflow]
    .slice(-80)
    .reverse()
    .map((row) => ({
      snapshotTime: row.snapshotTime,
      value: row.orderInflow15m,
      status: dashboard.getInflowStatus(row.orderInflow15m, dashboard.locale),
    })),
)

function toneClass(tone) {
  return `is-${tone || 'muted'}`
}
</script>

<template>
  <section class="page">
    <header class="header">
      <div>
        <p class="kicker">{{ dashboard.text.nav.demandPressure }}</p>
        <h1>{{ dashboard.text.orderInflow }}</h1>
        <p class="subtitle">
          {{ dashboard.locale === 'ko'
            ? '선택한 스냅샷 시점까지만 집계해 수요 압력을 계산합니다.'
            : 'Demand pressure is computed only from arrived orders up to the selected snapshot.' }}
        </p>
      </div>
      <div class="header-badges">
        <StatusBadge :label="dashboard.inflowStatus.label" :tone="dashboard.inflowStatus.tone" />
        <div class="header-metric">
          <span>{{ dashboard.locale === 'ko' ? '현재(15m)' : 'Current (15m)' }}</span>
          <strong>{{ dashboard.formatNumber(dashboard.selectedOrderInflow?.orderInflow15m, 1) }} {{ dashboard.locale === 'ko' ? '건' : 'orders' }}</strong>
        </div>
      </div>
    </header>

    <section class="grid">
      <article class="card span-2">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '주문 유입 추이' : 'Inflow trend' }}</p>
          <InfoHint
            :title="dashboard.text.orderInflow"
            :body="dashboard.locale === 'ko'
              ? '차트는 선택한 스냅샷 시점까지만 표시합니다.'
              : 'The chart stops at the selected snapshot.'"
            :points="dashboard.locale === 'ko'
              ? ['커서를 올리면 가장 가까운 값(15m)을 확인할 수 있습니다.']
              : ['Hover to inspect the closest 15m reading.']"
            align="right"
          />
        </div>
        <TimeSeriesChart
          :labels="dashboard.visibleSnapshotOptions"
          :display-labels="dashboard.snapshotLabels"
          :selected-label="dashboard.selectedSnapshot"
          :series="dashboard.inflowSeries"
          :value-formatter="(value) => dashboard.formatOrders(value)"
          :tooltip-label-formatter="(value) => dashboard.formatTimestamp(value, dashboard.locale)"
          :empty-label="dashboard.locale === 'ko' ? '표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'"
        />
      </article>

      <article class="card mini-card">
        <p class="mini-kicker">15m</p>
        <strong class="mini-value">{{ dashboard.formatNumber(dashboard.inflowCards.current15m, 1) }}</strong>
        <p class="mini-note">{{ dashboard.locale === 'ko' ? '선택 스냅샷 기준' : 'At selected snapshot' }}</p>
      </article>

      <article class="card mini-card">
        <p class="mini-kicker">30m</p>
        <strong class="mini-value">{{ dashboard.formatNumber(dashboard.inflowCards.current30m, 1) }}</strong>
        <p class="mini-note">{{ dashboard.locale === 'ko' ? '최근 2구간 합계' : 'Rolling sum (2 points)' }}</p>
      </article>

      <article class="card mini-card">
        <p class="mini-kicker">1h</p>
        <strong class="mini-value">{{ dashboard.formatNumber(dashboard.inflowCards.current60m, 1) }}</strong>
        <p class="mini-note">{{ dashboard.locale === 'ko' ? '최근 4구간 합계' : 'Rolling sum (4 points)' }}</p>
      </article>

      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.text.distribution }}</p>
        </div>
        <div class="dist-shell">
          <DonutChart :segments="inflowDistribution" :size="10.5" :hole="5.6">
            <div class="donut-center">
              <strong>{{ dashboard.text.demandPressure }}</strong>
              <span>{{ dashboard.timeWindowLabel }}</span>
            </div>
          </DonutChart>
        </div>
        <p class="mini-note">
          {{ dashboard.locale === 'ko'
            ? '분포는 선택 스냅샷까지의 수요 압력 톤을 집계한 결과입니다.'
            : 'Distribution counts demand pressure tones up to the selected snapshot.' }}
        </p>
      </article>
    </section>

    <section class="card">
      <div class="card-top">
        <p class="card-kicker">{{ dashboard.text.fullLog }}</p>
        <InfoHint :title="dashboard.helpContent.logs.title" :body="dashboard.helpContent.logs.body" :points="dashboard.helpContent.logs.points" align="right" />
      </div>

      <div class="table-shell">
        <table>
          <thead>
            <tr>
              <th>{{ dashboard.text.timestamp }}</th>
              <th>{{ dashboard.text.value }}</th>
              <th>{{ dashboard.text.status }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in inflowLog" :key="row.snapshotTime">
              <td class="cell-time">{{ dashboard.formatTimestamp(row.snapshotTime, dashboard.locale) }}</td>
              <td>{{ dashboard.formatOrders(row.value) }}</td>
              <td>
                <span class="tone-dot" :class="toneClass(row.status.tone)" aria-hidden="true"></span>
                <span class="status-label">{{ row.status.label }}</span>
              </td>
            </tr>
            <tr v-if="!inflowLog.length">
              <td class="empty-row" colspan="3">{{ dashboard.text.noData }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 1.2rem;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
  padding: 1.25rem 1.35rem;
  border-radius: 2rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.kicker {
  color: #c2410c;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.header h1 {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 2.2rem;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
  margin-top: 0.25rem;
}

.subtitle {
  margin-top: 0.55rem;
  max-width: 48rem;
  color: #475569;
  line-height: 1.6;
}

.header-badges {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-metric {
  padding: 0.75rem 0.9rem;
  border-radius: 1.25rem;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
  min-width: 12rem;
}

.header-metric span {
  display: block;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.header-metric strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.02rem;
  font-weight: 900;
  color: #0f172a;
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.span-2 {
  grid-column: span 2;
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

.mini-card {
  align-content: start;
  gap: 0.35rem;
}

.mini-kicker {
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.mini-value {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 2.2rem;
  line-height: 1;
  font-weight: 900;
  color: #0f172a;
}

.mini-note {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.55;
}

.dist-shell {
  display: grid;
  place-items: center;
  padding: 0.5rem 0;
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

.table-shell {
  overflow: auto;
  border-radius: 1.25rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 34rem;
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

.status-label {
  font-weight: 900;
  color: #0f172a;
}

.empty-row {
  color: #64748b;
  text-align: center;
  font-weight: 800;
}

@media (max-width: 1100px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>

