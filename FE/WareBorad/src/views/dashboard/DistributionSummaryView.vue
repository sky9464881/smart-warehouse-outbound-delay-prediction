<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useAuthStore } from '@/stores/auth'
import { getFactoriesOverview } from '@/api/dashboard'
import DonutChart from '@/components/DonutChart.vue'
import InfoHint from '@/components/InfoHint.vue'

const dashboard = useDashboardStore()
const auth = useAuthStore()

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

const overviewFactories = ref([])
const isOverviewLoading = ref(false)
const overviewErrorMessage = ref('')
const overviewProgress = ref(0)
const overviewProgressTimer = ref(null)

const distScope = ref(auth.isGlobalAdmin ? 'all' : 'selected')

function clearOverviewProgressTimer() {
  if (overviewProgressTimer.value) {
    window.clearInterval(overviewProgressTimer.value)
    overviewProgressTimer.value = null
  }
}

async function loadFactoriesOverview() {
  if (isOverviewLoading.value) return

  const controller = new AbortController()
  const timeoutMs = 15000
  let timeoutId = null

  isOverviewLoading.value = true
  overviewErrorMessage.value = ''
  overviewFactories.value = []
  overviewProgress.value = 4
  clearOverviewProgressTimer()
  overviewProgressTimer.value = window.setInterval(() => {
    if (overviewProgress.value < 90) {
      overviewProgress.value += 6
    } else {
      overviewProgress.value = 90
    }
  }, 500)

  timeoutId = window.setTimeout(() => {
    controller.abort()
  }, timeoutMs)

  try {
    overviewFactories.value = await getFactoriesOverview({ signal: controller.signal })
    if (!overviewFactories.value.length) {
      overviewErrorMessage.value = dashboard.locale === 'ko'
        ? '전체 공장의 최신 스냅샷을 찾을 수 없습니다.'
        : 'No latest snapshots found for all factories.'
    }
  } catch (error) {
    overviewFactories.value = []
    if (error?.name === 'AbortError') {
      overviewErrorMessage.value = dashboard.locale === 'ko'
        ? '전체 공장 데이터를 불러오는 데 시간이 너무 오래 걸렸습니다. 다시 시도하세요.'
        : 'Loading all factories data took too long. Please retry.'
    } else {
      overviewErrorMessage.value = error.message || (dashboard.locale === 'ko' ? '전체 공장 요약을 불러오지 못했습니다.' : 'Failed to load factory overview.')
    }
  } finally {
    if (timeoutId) {
      window.clearTimeout(timeoutId)
    }
    overviewProgress.value = 100
    clearOverviewProgressTimer()
    isOverviewLoading.value = false
  }
}

onUnmounted(() => {
  clearOverviewProgressTimer()
})

onMounted(() => {
  if (auth.isGlobalAdmin) {
    loadFactoriesOverview()
  }
})

watch(
  () => auth.isGlobalAdmin,
  (isAdmin) => {
    if (isAdmin && distScope.value === 'all') {
      loadFactoriesOverview()
    }
  },
)

watch(
  () => distScope.value,
  (scope) => {
    if (scope === 'all' && auth.isGlobalAdmin) {
      loadFactoriesOverview()
    }
  },
)

const overviewFactoryCount = computed(() => overviewFactories.value.length)

const overviewMaxCongestionScore = computed(() => {
  const values = overviewFactories.value.map((entry) => Number(entry.congestionScore)).filter((value) => Number.isFinite(value))
  return values.length ? Math.max(...values) : 0
})

const overviewDelayDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of overviewFactories.value) {
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

const overviewInflowDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of overviewFactories.value) {
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

const overviewCongestionDistribution = computed(() => {
  const counts = { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
  for (const row of overviewFactories.value) {
    const tone = dashboard.getCongestionTone(row.congestionScore, overviewMaxCongestionScore.value)
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

const isAllFactoryScope = computed(() => auth.isGlobalAdmin && distScope.value === 'all')

const distDelaySegments = computed(() => (isAllFactoryScope.value ? overviewDelayDistribution.value : delayDistribution.value))
const distInflowSegments = computed(() => (isAllFactoryScope.value ? overviewInflowDistribution.value : inflowDistribution.value))
const distCongestionSegments = computed(() => (isAllFactoryScope.value ? overviewCongestionDistribution.value : congestionDistribution.value))

const distributionScopeLabel = computed(() => {
  if (isAllFactoryScope.value) {
    return dashboard.locale === 'ko'
      ? `전체 공장 (${overviewFactoryCount.value}개)`
      : `All factories (${overviewFactoryCount.value})`
  }
  return dashboard.timeWindowLabel
})

const distributionNote = computed(() => {
  if (isAllFactoryScope.value) {
    return dashboard.locale === 'ko'
      ? '각 공장의 최신 스냅샷 기준으로 상태 톤(정상/주의/경고/위험) 분포를 집계합니다.'
      : 'Counts status tones (good/watch/warning/critical) using the latest snapshot per factory.'
  }

  return dashboard.locale === 'ko'
    ? '분포는 선택 스냅샷까지의 상태 톤(좋음/주의/경고/위험)을 집계한 결과입니다.'
    : 'Distributions count status tones (good/watch/warning/critical) up to the selected snapshot.'
})

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
</script>

<template>
  <section class="page">
    <header class="hero">
      <p class="hero-kicker">{{ dashboard.text.appTagline }}</p>
      <h1>{{ dashboard.locale === 'ko' ? '전체 분포 요약' : 'Distribution Summary' }}</h1>
      <p class="hero-subtitle">{{ dashboard.locale === 'ko' ? '전체 공장 또는 선택 공장의 상태 분포를 확인하세요.' : 'Check status distributions across all factories or selected factories.' }}</p>
    </header>

    <section v-if="dashboard.errorMessage" class="message-card is-error">
      <strong>{{ dashboard.locale === 'ko' ? '대시보드 데이터를 불러오지 못했습니다.' : 'Dashboard data could not be loaded.' }}</strong>
      <p>{{ dashboard.errorMessage }}</p>
      <button class="retry-button" type="button" @click="dashboard.bootstrap">{{ dashboard.text.reloadDashboard }}</button>
    </section>

    <section v-else-if="dashboard.isBooting" class="message-card">
      <p>{{ dashboard.text.preparingDashboard }}</p>
    </section>

    <section v-else-if="!dashboard.factories.length" class="message-card">
      <strong>{{ dashboard.locale === 'ko' ? '접근 가능한 공장이 없습니다.' : 'No factories assigned.' }}</strong>
      <p>
        {{ dashboard.locale === 'ko'
          ? '전체 관리자에게 공장 접근 권한을 요청하세요.'
          : 'Ask a global admin to grant factory access.' }}
      </p>
    </section>

    <template v-else>
      <section class="grid lower-grid">
        <article class="card dist-card">
          <div class="card-top">
            <p class="card-kicker">{{ dashboard.text.distribution }}</p>
            <div v-if="auth.isGlobalAdmin" class="dist-toggle" role="group" aria-label="distribution scope">
              <button type="button" class="dist-button" :class="{ 'is-active': distScope === 'all' }" @click="distScope = 'all'">
                {{ dashboard.locale === 'ko' ? '전체 공장' : 'All' }}
              </button>
              <button type="button" class="dist-button" :class="{ 'is-active': distScope === 'selected' }" @click="distScope = 'selected'">
                {{ dashboard.locale === 'ko' ? '선택 공장' : 'Selected' }}
              </button>
            </div>
          </div>
          <div v-if="isAllFactoryScope && isOverviewLoading" class="dist-loading-block">
            <p>{{ dashboard.locale === 'ko' ? '전체 공장 분포를 불러오는 중입니다...' : 'Loading all factories distribution...' }}</p>
            <div class="dist-progress">
              <span>{{ overviewProgress }}%</span>
              <progress :value="overviewProgress" max="100"></progress>
            </div>
          </div>
          <div v-else-if="isAllFactoryScope && overviewErrorMessage" class="dist-state is-error">
            <p>{{ overviewErrorMessage }}</p>
            <button type="button" class="retry-button" @click="loadFactoriesOverview">
              {{ dashboard.locale === 'ko' ? '다시 시도' : 'Retry' }}
            </button>
          </div>
          <div v-else class="dist-grid">
            <div class="dist-item">
              <DonutChart :segments="distDelaySegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.shippingDelay }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
            </div>
            <div class="dist-item">
              <DonutChart :segments="distInflowSegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.demandPressure }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
            </div>
            <div class="dist-item">
              <DonutChart :segments="distCongestionSegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.flowFriction }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
            </div>
          </div>
          <p class="dist-note">
            {{ distributionNote }}
          </p>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.dist-loading-block {
  display: grid;
  gap: 1rem;
  padding: 1.5rem 0;
}

.dist-loading-block p {
  margin: 0;
  font-weight: 600;
  color: #334155;
}

.dist-progress {
  display: grid;
  gap: 0.5rem;
}

.dist-progress span {
  font-size: 0.95rem;
  color: #475569;
}

.dist-progress progress {
  width: 100%;
  height: 0.75rem;
  appearance: none;
  border: none;
  border-radius: 999px;
  overflow: hidden;
  background: #e2e8f0;
}

.dist-progress progress::-webkit-progress-bar {
  background: #e2e8f0;
}

.dist-progress progress::-webkit-progress-value {
  background: #0f766e;
}

.dist-progress progress::-moz-progress-bar {
  background: #0f766e;
}

.retry-button {
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1rem;
  background: #0f172a;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  width: fit-content;
}
</style>