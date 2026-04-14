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

const congestionDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of dashboard.visibleCongestion) {
    const tone = dashboard.getCongestionTone(row.congestionScore, dashboard.maxCongestionScore)
    counts[tone] = (counts[tone] ?? 0) + 1
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

const congestionLog = computed(() =>
  [...dashboard.visibleCongestion]
    .slice(-80)
    .reverse()
    .map((row) => ({
      snapshotTime: row.snapshotTime,
      value: row.congestionScore,
      tone: dashboard.getCongestionTone(row.congestionScore, dashboard.maxCongestionScore),
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
        <p class="kicker">{{ dashboard.text.nav.flowFriction }}</p>
        <h1>{{ dashboard.text.congestionScore }}</h1>
        <p class="subtitle">
          {{ dashboard.locale === 'ko'
            ? '선택한 스냅샷 시점까지의 혼잡도와 흐름 마찰도를 추적합니다.'
            : 'Track congestion and flow friction up to the selected snapshot.' }}
        </p>
      </div>
      <div class="header-badges">
        <StatusBadge :label="dashboard.congestionStatus.label" :tone="dashboard.congestionStatus.tone" />
        <div class="header-metric">
          <span>{{ dashboard.locale === 'ko' ? '현재 점수' : 'Current score' }}</span>
          <strong>{{ dashboard.formatNumber(dashboard.selectedCongestion?.congestionScore, 1) }}</strong>
        </div>
      </div>
    </header>

    <section class="grid">
      <article class="card span-2">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '혼잡도 추이' : 'Congestion trend' }}</p>
          <InfoHint
            :title="dashboard.text.flowFriction"
            :body="dashboard.locale === 'ko'
              ? '차트는 선택한 스냅샷 시점까지의 혼잡도 점수만 표시합니다.'
              : 'The chart stops at the selected snapshot.'"
            :points="dashboard.locale === 'ko'
              ? ['커서를 올리면 가장 가까운 시점 값을 확인할 수 있습니다.']
              : ['Hover to inspect the nearest observed value.']"
            align="right"
          />
        </div>
        <TimeSeriesChart
          :labels="dashboard.visibleSnapshotOptions"
          :display-labels="dashboard.snapshotLabels"
          :selected-label="dashboard.selectedSnapshot"
          :series="dashboard.congestionSeries"
          :value-formatter="(value) => dashboard.formatNumber(value, 1)"
          :tooltip-label-formatter="(value) => dashboard.formatTimestamp(value, dashboard.locale)"
          :empty-label="dashboard.locale === 'ko' ? '표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'"
        />
      </article>

      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.text.distribution }}</p>
        </div>
        <div class="dist-shell">
          <DonutChart :segments="congestionDistribution" :size="10.5" :hole="5.6">
            <div class="donut-center">
              <strong>{{ dashboard.text.flowFriction }}</strong>
              <span>{{ dashboard.timeWindowLabel }}</span>
            </div>
          </DonutChart>
        </div>
        <p class="note">
          {{ dashboard.locale === 'ko'
            ? '분포는 표시 구간 내 최고값 대비 상대 톤(좋음/주의/경고/위험)을 집계한 결과입니다.'
            : 'Distribution counts relative tones compared to the peak score in the visible window.' }}
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
            <tr v-for="row in congestionLog" :key="row.snapshotTime">
              <td class="cell-time">{{ dashboard.formatTimestamp(row.snapshotTime, dashboard.locale) }}</td>
              <td>{{ dashboard.formatNumber(row.value, 1) }}</td>
              <td>
                <span class="tone-dot" :class="toneClass(row.tone)" aria-hidden="true"></span>
                <span class="status-label">{{ row.tone }}</span>
              </td>
            </tr>
            <tr v-if="!congestionLog.length">
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
  min-width: 10.5rem;
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
  align-items: start;
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

.note {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.55;
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
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 0.78rem;
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

