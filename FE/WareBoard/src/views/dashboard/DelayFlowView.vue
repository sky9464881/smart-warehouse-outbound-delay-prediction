<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import InfoHint from '@/components/InfoHint.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TimeSeriesChart from '@/components/TimeSeriesChart.vue'

const dashboard = useDashboardStore()

const cumulativeDelaySeries = computed(() => {
  let total = 0
  const values = dashboard.visibleSnapshotOptions.map((snapshotTime) => {
    const value = dashboard.metricAt(dashboard.visibleShippingDelay, snapshotTime, 'avgDelayMinutesNext30m')
    if (value == null) return null
    total += Number(value)
    return total
  })

  return [{
    key: 'cumulativeDelay',
    label: dashboard.locale === 'ko' ? '누적 지연' : 'Cumulative delay',
    color: '#2563eb',
    values,
  }]
})

const delayLog = computed(() =>
  [...dashboard.visibleShippingDelay]
    .slice(-60)
    .reverse()
    .map((row) => ({
      snapshotTime: row.snapshotTime,
      value: row.avgDelayMinutesNext30m,
      status: dashboard.getDelayStatus(row.avgDelayMinutesNext30m, dashboard.locale),
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
        <p class="kicker">{{ dashboard.text.nav.delayFlow }}</p>
        <h1>{{ dashboard.text.shippingDelay }}</h1>
        <p class="subtitle">
          {{ dashboard.locale === 'ko'
            ? '선택한 스냅샷 시점까지의 지연 추이와 누적 흐름을 확인합니다.'
            : 'Inspect the delay trend and the cumulative flow up to the selected snapshot.' }}
        </p>
      </div>
      <div class="header-badges">
        <StatusBadge :label="dashboard.delayStatus.label" :tone="dashboard.delayStatus.tone" />
        <div class="header-metric">
          <span>{{ dashboard.locale === 'ko' ? '현재' : 'Current' }}</span>
          <strong>{{ dashboard.formatMinutes(dashboard.selectedShippingDelay?.avgDelayMinutesNext30m) }}</strong>
        </div>
      </div>
    </header>

    <section class="grid">
      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '지연 추이' : 'Delay trend' }}</p>
          <InfoHint
            :title="dashboard.text.shippingDelay"
            :body="dashboard.locale === 'ko' ? '차트는 선택한 스냅샷 시점까지의 지연값만 표시합니다.' : 'The chart shows delay values only up to the selected snapshot.'"
            :points="dashboard.locale === 'ko' ? ['커서를 올리면 가장 가까운 시점 값을 볼 수 있습니다.'] : ['Hover to inspect the nearest observed value.']"
            align="right"
          />
        </div>
        <TimeSeriesChart
          :labels="dashboard.visibleSnapshotOptions"
          :display-labels="dashboard.snapshotLabels"
          :selected-label="dashboard.selectedSnapshot"
          :series="dashboard.delaySeries"
          :value-formatter="(value) => dashboard.formatMinutes(value)"
          :tooltip-label-formatter="(value) => dashboard.formatTimestamp(value, dashboard.locale)"
          :empty-label="dashboard.locale === 'ko' ? '표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'"
        />
      </article>

      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '지연 누적 흐름' : 'Cumulative flow' }}</p>
          <InfoHint
            :title="dashboard.locale === 'ko' ? '누적 지연' : 'Cumulative delay'"
            :body="dashboard.locale === 'ko'
              ? '선택 구간 내 지연값을 시간 순서대로 누적한 흐름입니다.'
              : 'A running accumulation of delay values across the visible window.'"
            :points="dashboard.locale === 'ko'
              ? ['가파르게 상승하면 지연이 계속 쌓이고 있음을 의미합니다.']
              : ['A steep climb usually means delay is building up continuously.']"
            align="right"
          />
        </div>
        <TimeSeriesChart
          :labels="dashboard.visibleSnapshotOptions"
          :display-labels="dashboard.snapshotLabels"
          :selected-label="dashboard.selectedSnapshot"
          :series="cumulativeDelaySeries"
          :value-formatter="(value) => dashboard.formatMinutes(value)"
          :tooltip-label-formatter="(value) => dashboard.formatTimestamp(value, dashboard.locale)"
          :empty-label="dashboard.locale === 'ko' ? '표시할 시계열 데이터가 없습니다.' : 'No time-series data for this scenario.'"
        />
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
            <tr v-for="row in delayLog" :key="row.snapshotTime">
              <td class="cell-time">{{ dashboard.formatTimestamp(row.snapshotTime, dashboard.locale) }}</td>
              <td>{{ dashboard.formatMinutes(row.value) }}</td>
              <td>
                <span class="tone-dot" :class="toneClass(row.status.tone)" aria-hidden="true"></span>
                <span class="status-label">{{ row.status.label }}</span>
              </td>
            </tr>
            <tr v-if="!delayLog.length">
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
  font-size: 1.05rem;
  font-weight: 900;
  color: #0f172a;
}

.grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
}
</style>

