<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import DonutChart from '@/components/DonutChart.vue'
import InfoHint from '@/components/InfoHint.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import TimeSeriesChart from '@/components/TimeSeriesChart.vue'

const dashboard = useDashboardStore()

const robotLog = computed(() =>
  [...dashboard.visibleRobotSummary]
    .slice(-80)
    .reverse()
    .map((row) => ({
      snapshotTime: row.snapshotTime,
      active: row.robotActive,
      idle: row.robotIdle,
      charging: row.robotCharging,
      avgIdle: row.avgIdleDurationMin,
    })),
)

const spareShare = computed(() => {
  const { idle, charging, total } = dashboard.robotTotals
  if (!total) return null
  return ((idle + charging) / total) * 100
})
</script>

<template>
  <section class="page">
    <header class="header">
      <div>
        <p class="kicker">{{ dashboard.text.nav.capacity }}</p>
        <h1>{{ dashboard.text.robotStateMix }}</h1>
        <p class="subtitle">
          {{ dashboard.locale === 'ko'
            ? '선택한 스냅샷 시점의 로봇 상태 믹스와 가동 여력을 확인합니다.'
            : 'Review the robot posture and spare capacity at the selected snapshot.' }}
        </p>
      </div>
      <div class="header-badges">
        <StatusBadge :label="dashboard.capacityStatus.label" :tone="dashboard.capacityStatus.tone" />
        <div class="header-metric">
          <span>{{ dashboard.locale === 'ko' ? '유휴·충전 비율' : 'Idle + charging share' }}</span>
          <strong>{{ spareShare == null ? '-' : `${dashboard.formatNumber(spareShare, 1)}%` }}</strong>
        </div>
      </div>
    </header>

    <section class="grid">
      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '상태 구성(스냅샷)' : 'State mix (snapshot)' }}</p>
          <InfoHint
            :title="dashboard.text.robotStateMix"
            :body="dashboard.locale === 'ko'
              ? '도넛은 선택한 스냅샷 시점의 로봇 상태 분포를 보여줍니다.'
              : 'The donut shows the robot state distribution at the selected snapshot.'"
            :points="dashboard.locale === 'ko'
              ? ['유휴·충전이 높으면 여력이 크고, 가동이 높으면 여력이 줄어듭니다.']
              : ['More idle/charging typically means more spare capacity; more active means tighter posture.']"
            align="right"
          />
        </div>

        <div class="donut-shell">
          <DonutChart :segments="dashboard.robotSegments" :size="14.5" :hole="8.2">
            <div class="donut-center">
              <strong>{{ dashboard.formatNumber(dashboard.robotTotals.total) }}</strong>
              <span>{{ dashboard.text.robotsLive }}</span>
            </div>
          </DonutChart>
        </div>

        <div class="legend">
          <div v-for="segment in dashboard.robotSegments" :key="segment.key" class="legend-item">
            <span class="legend-dot" :style="{ background: segment.color }" aria-hidden="true"></span>
            <span class="legend-label">{{ segment.label }}</span>
            <strong class="legend-value">{{ dashboard.formatNumber(segment.value) }}</strong>
          </div>
        </div>
      </article>

      <article class="card span-2">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '상태 추이' : 'State trend' }}</p>
          <InfoHint
            :title="dashboard.locale === 'ko' ? '로봇 상태 추이' : 'Robot state trend'"
            :body="dashboard.locale === 'ko'
              ? '선택 스냅샷까지의 상태 변화를 확인합니다.'
              : 'Shows how the posture evolved up to the selected snapshot.'"
            :points="dashboard.locale === 'ko'
              ? ['커서를 올리면 가장 가까운 시점의 상태 값을 볼 수 있습니다.']
              : ['Hover to inspect the nearest snapshot values.']"
            align="right"
          />
        </div>
        <TimeSeriesChart
          :labels="dashboard.visibleSnapshotOptions"
          :display-labels="dashboard.snapshotLabels"
          :selected-label="dashboard.selectedSnapshot"
          :series="dashboard.robotSeries"
          :value-formatter="(value) => dashboard.formatNumber(value)"
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
              <th>{{ dashboard.text.active }}</th>
              <th>{{ dashboard.text.idle }}</th>
              <th>{{ dashboard.text.charging }}</th>
              <th>{{ dashboard.text.avgIdle }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in robotLog" :key="row.snapshotTime">
              <td class="cell-time">{{ dashboard.formatTimestamp(row.snapshotTime, dashboard.locale) }}</td>
              <td>{{ dashboard.formatNumber(row.active) }}</td>
              <td>{{ dashboard.formatNumber(row.idle) }}</td>
              <td>{{ dashboard.formatNumber(row.charging) }}</td>
              <td>{{ dashboard.formatMinutes(row.avgIdle) }}</td>
            </tr>
            <tr v-if="!robotLog.length">
              <td class="empty-row" colspan="5">{{ dashboard.text.noData }}</td>
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

.donut-shell {
  display: grid;
  place-items: center;
  padding: 0.5rem 0;
}

.donut-center strong {
  display: block;
  font-weight: 900;
  font-size: 2.1rem;
  color: #0f172a;
  line-height: 1;
}

.donut-center span {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.legend {
  display: grid;
  gap: 0.5rem;
}

.legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.7rem;
  border-radius: 1rem;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.legend-dot {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 999px;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.05);
  margin-right: 0.5rem;
}

.legend-label {
  flex: 1;
  font-weight: 900;
  color: #0f172a;
}

.legend-value {
  font-weight: 900;
  color: #475569;
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
  min-width: 40rem;
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

