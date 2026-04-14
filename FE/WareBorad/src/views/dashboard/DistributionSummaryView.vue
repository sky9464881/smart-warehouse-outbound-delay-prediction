<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
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

const OVERVIEW_SOFT_TIMEOUT_MS = 15000
const OVERVIEW_HARD_TIMEOUT_MS = 90000
const OVERVIEW_CACHE_TTL_MS = 5 * 60 * 1000

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

function emptyToneCounts() {
  return { good: 0, watch: 0, warning: 0, critical: 0, muted: 0 }
}

function buildToneReport(counts, labels) {
  const entries = [
    { key: 'good', label: labels.good, value: counts.good },
    { key: 'watch', label: labels.watch, value: counts.watch },
    { key: 'warning', label: labels.warning, value: counts.warning },
    { key: 'critical', label: labels.critical, value: counts.critical },
    { key: 'muted', label: labels.muted, value: counts.muted },
  ]

  return {
    counts,
    total: entries.reduce((sum, entry) => sum + entry.value, 0),
    segments: buildToneSegments(entries),
  }
}

function buildToneReportFromRows(rows, toneGetter, labels) {
  const counts = emptyToneCounts()
  for (const row of rows) {
    const tone = toneGetter(row)
    counts[tone] = (counts[tone] ?? 0) + 1
  }
  return buildToneReport(counts, labels)
}

const overviewFactories = ref([])
const overviewLoadedAt = ref(0)
const isOverviewLoading = ref(false)
const overviewSlow = ref(false)
const overviewErrorMessage = ref('')
const overviewProgress = ref(0)
const overviewProgressTimer = ref(null)
const overviewAbortController = ref(null)
const overviewSoftTimeoutId = ref(null)
const overviewHardTimeoutId = ref(null)
let overviewRequest = 0

function clearOverviewProgressTimer() {
  if (overviewProgressTimer.value) {
    window.clearInterval(overviewProgressTimer.value)
    overviewProgressTimer.value = null
  }
}

function clearOverviewTimeouts() {
  if (overviewSoftTimeoutId.value) {
    window.clearTimeout(overviewSoftTimeoutId.value)
    overviewSoftTimeoutId.value = null
  }
  if (overviewHardTimeoutId.value) {
    window.clearTimeout(overviewHardTimeoutId.value)
    overviewHardTimeoutId.value = null
  }
}

function cancelFactoriesOverview() {
  overviewRequest += 1
  if (overviewAbortController.value) {
    overviewAbortController.value.abort()
  }
  overviewAbortController.value = null
  clearOverviewProgressTimer()
  clearOverviewTimeouts()
  isOverviewLoading.value = false
  overviewSlow.value = false
  overviewProgress.value = 0
  overviewErrorMessage.value = ''
}

async function loadFactoriesOverview(options = {}) {
  if (isOverviewLoading.value) return

  const { force = false } = options
  const now = Date.now()
  if (!force && overviewFactories.value.length && now - overviewLoadedAt.value < OVERVIEW_CACHE_TTL_MS) {
    overviewErrorMessage.value = ''
    return
  }

  const requestId = ++overviewRequest
  const controller = new AbortController()
  overviewAbortController.value = controller

  isOverviewLoading.value = true
  overviewSlow.value = false
  overviewErrorMessage.value = ''
  overviewProgress.value = 4

  clearOverviewProgressTimer()
  overviewProgressTimer.value = window.setInterval(() => {
    if (overviewProgress.value < 95) {
      overviewProgress.value += overviewSlow.value ? 2 : 5
    } else {
      overviewProgress.value = 95
    }
  }, 600)

  clearOverviewTimeouts()
  overviewSoftTimeoutId.value = window.setTimeout(() => {
    if (requestId !== overviewRequest) return
    overviewSlow.value = true
  }, OVERVIEW_SOFT_TIMEOUT_MS)

  overviewHardTimeoutId.value = window.setTimeout(() => {
    if (requestId !== overviewRequest) return
    controller.abort()
  }, OVERVIEW_HARD_TIMEOUT_MS)

  try {
    const payload = await getFactoriesOverview({ signal: controller.signal })
    if (requestId !== overviewRequest) return

    overviewFactories.value = Array.isArray(payload) ? payload : []
    overviewLoadedAt.value = Date.now()

    if (!overviewFactories.value.length) {
      overviewErrorMessage.value = dashboard.locale === 'ko'
        ? '접근 가능한 공장의 최신 스냅샷을 찾을 수 없습니다.'
        : 'No latest snapshots found for accessible factories.'
    }
  } catch (error) {
    if (requestId !== overviewRequest) return

    if (error?.name === 'AbortError') {
      overviewErrorMessage.value = dashboard.locale === 'ko'
        ? '공장 분포 데이터를 불러오는 데 시간이 너무 오래 걸렸습니다. 잠시 후 다시 시도해주세요.'
        : 'Loading factory distribution data took too long. Please retry.'
    } else {
      overviewErrorMessage.value = error?.message || (dashboard.locale === 'ko' ? '공장 분포 요약을 불러오지 못했습니다.' : 'Failed to load factory overview.')
    }
  } finally {
    if (requestId !== overviewRequest) return
    overviewProgress.value = 100
    clearOverviewProgressTimer()
    clearOverviewTimeouts()
    overviewAbortController.value = null
    isOverviewLoading.value = false
    overviewSlow.value = false
  }
}

onUnmounted(() => {
  cancelFactoriesOverview()
})

onMounted(() => {
  loadFactoriesOverview()
})

const accessScopeLabel = computed(() => {
  const isKo = dashboard.locale === 'ko'
  if (auth.isGlobalAdmin) {
    return isKo ? '전 공장' : 'All factories'
  }
  return isKo ? '권한 공장' : 'Accessible factories'
})

const overviewFactoryCount = computed(() => overviewFactories.value.length)

const overviewMaxCongestionScore = computed(() => {
  const values = overviewFactories.value.map((entry) => Number(entry.congestionScore)).filter((value) => Number.isFinite(value))
  return values.length ? Math.max(...values) : 0
})

const toneLabels = computed(() => {
  const isKo = dashboard.locale === 'ko'

  return {
    delay: {
      good: isKo ? '정상' : 'Normal',
      watch: isKo ? '주의' : 'Watch',
      warning: isKo ? '경고' : 'Warning',
      critical: isKo ? '위험' : 'Critical',
      muted: isKo ? '데이터 없음' : 'No data',
    },
    inflow: {
      good: isKo ? '낮음' : 'Low',
      watch: isKo ? '보통' : 'Moderate',
      warning: isKo ? '높음' : 'High',
      critical: isKo ? '매우 높음' : 'Very high',
      muted: isKo ? '데이터 없음' : 'No data',
    },
    congestion: {
      good: isKo ? '원활' : 'Smooth',
      watch: isKo ? '주의' : 'Watch',
      warning: isKo ? '혼잡' : 'Congested',
      critical: isKo ? '심각' : 'Severe',
      muted: isKo ? '데이터 없음' : 'No data',
    },
  }
})

const overviewDelayReport = computed(() =>
  buildToneReportFromRows(
    overviewFactories.value,
    (row) => dashboard.getDelayStatus(row.avgDelayMinutesNext30m, dashboard.locale).tone,
    toneLabels.value.delay,
  ),
)

const overviewInflowReport = computed(() =>
  buildToneReportFromRows(
    overviewFactories.value,
    (row) => dashboard.getInflowStatus(row.orderInflow15m, dashboard.locale).tone,
    toneLabels.value.inflow,
  ),
)

const overviewCongestionReport = computed(() =>
  buildToneReportFromRows(
    overviewFactories.value,
    (row) => dashboard.getCongestionTone(row.congestionScore, overviewMaxCongestionScore.value),
    toneLabels.value.congestion,
  ),
)

const distDelayReport = overviewDelayReport
const distInflowReport = overviewInflowReport
const distCongestionReport = overviewCongestionReport

const distDelaySegments = computed(() => distDelayReport.value.segments)
const distInflowSegments = computed(() => distInflowReport.value.segments)
const distCongestionSegments = computed(() => distCongestionReport.value.segments)

const distributionScopeLabel = computed(() => {
  const count = overviewFactoryCount.value
  const label = accessScopeLabel.value
  return dashboard.locale === 'ko' ? `${label} (${count}개)` : `${label} (${count})`
})

const distributionNote = computed(() => {
  if (dashboard.locale === 'ko') {
    return `이 탭은 상단 공장 선택과 무관하게, ${accessScopeLabel.value}의 각 최신 스냅샷 1개로 상태 톤(정상/주의/경고/위험) 분포를 집계합니다.`
  }
  return 'Aggregates tone distributions using the latest snapshot per factory in your access scope (independent from the factory selector).'
})

const distributionHintPoints = computed(() => {
  const isKo = dashboard.locale === 'ko'
  if (isKo) {
    return [
      '이 탭은 상단 공장 선택과 무관하게 권한 공장을 집계합니다.',
      '도넛은 각 톤의 비중(%)을 보여줍니다.',
      '경고/위험 비중이 높을수록 운영 리스크가 큽니다.',
      '데이터 없음이 많다면 수집/집계 파이프라인을 점검하세요.',
    ]
  }
  return [
    'This tab aggregates all accessible factories (independent from the factory selector).',
    'Donuts show the share (%) of each tone.',
    'Higher warning/critical share usually means higher operational risk.',
    'A large “No data” slice may indicate ingestion or aggregation gaps.',
  ]
})

function toneWeight(tone) {
  if (tone === 'critical') return 3
  if (tone === 'warning') return 2
  if (tone === 'watch') return 1
  return 0
}

function strongestTone(counts) {
  if (counts.critical) return 'critical'
  if (counts.warning) return 'warning'
  if (counts.watch) return 'watch'
  if (counts.good) return 'good'
  return 'muted'
}

function toneClass(tone) {
  return `is-${tone || 'muted'}`
}

function toneBadgeLabel(tone) {
  const isKo = dashboard.locale === 'ko'
  if (isKo) {
    if (tone === 'critical') return '위험'
    if (tone === 'warning') return '경고'
    if (tone === 'watch') return '주의'
    if (tone === 'good') return '정상'
    return '데이터 없음'
  }

  if (tone === 'critical') return 'Critical'
  if (tone === 'warning') return 'Warning'
  if (tone === 'watch') return 'Watch'
  if (tone === 'good') return 'Good'
  return 'No data'
}

function toFiniteNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function averageFinite(rows, getter) {
  const values = rows.map((row) => toFiniteNumber(getter(row))).filter((value) => value != null)
  if (!values.length) return null
  return values.reduce((sum, value) => sum + value, 0) / values.length
}

function maxFinite(rows, getter) {
  let bestValue = null
  let bestRow = null
  for (const row of rows) {
    const value = toFiniteNumber(getter(row))
    if (value == null) continue
    if (bestValue == null || value > bestValue) {
      bestValue = value
      bestRow = row
    }
  }
  return bestRow ? { row: bestRow, value: bestValue } : null
}

function buildAlertSummary(report) {
  const total = report?.total ?? 0
  const warning = report?.counts?.warning ?? 0
  const critical = report?.counts?.critical ?? 0
  const alert = warning + critical
  const share = total ? Math.round((alert / total) * 100) : 0
  return { alert, total, warning, critical, share }
}

function formatAlertLine(summary) {
  if (!summary.total) {
    return dashboard.locale === 'ko' ? '표본 데이터가 없습니다.' : 'No samples available.'
  }

  const label = dashboard.locale === 'ko' ? '경고+위험' : 'Warning+Critical'
  return `${label}: ${summary.alert}/${summary.total} (${summary.share}%)`
}

const overviewLatestSnapshotTime = computed(() => {
  let best = null
  let bestMs = null
  for (const row of overviewFactories.value) {
    if (!row?.snapshotTime) continue
    const ms = new Date(row.snapshotTime).getTime()
    if (!Number.isFinite(ms)) continue
    if (bestMs == null || ms > bestMs) {
      bestMs = ms
      best = row.snapshotTime
    }
  }
  return best
})

const highlightMetaItems = computed(() => {
  const isKo = dashboard.locale === 'ko'

  return [
    { label: isKo ? '범위' : 'Scope', value: accessScopeLabel.value },
    { label: isKo ? '공장 수' : 'Factories', value: overviewFactoryCount.value || '-' },
    { label: isKo ? '최신 스냅샷' : 'Latest snapshot', value: overviewLatestSnapshotTime.value ? dashboard.formatTimestamp(overviewLatestSnapshotTime.value, dashboard.locale) : '-' },
    { label: isKo ? '업데이트' : 'Updated', value: overviewLoadedAt.value ? dashboard.formatTimestamp(overviewLoadedAt.value, dashboard.locale) : '-' },
  ]
})

const distAverageDelay = computed(() => averageFinite(overviewFactories.value, (row) => row.avgDelayMinutesNext30m))
const distPeakInflow = computed(() => maxFinite(overviewFactories.value, (row) => row.orderInflow15m)?.value ?? null)
const distAverageCongestion = computed(() => averageFinite(overviewFactories.value, (row) => row.congestionScore))

const distMaxDelay = computed(() => maxFinite(overviewFactories.value, (row) => row.avgDelayMinutesNext30m))
const distMaxInflow = computed(() => maxFinite(overviewFactories.value, (row) => row.orderInflow15m))
const distMaxCongestion = computed(() => maxFinite(overviewFactories.value, (row) => row.congestionScore))

const delayAlertSummary = computed(() => buildAlertSummary(distDelayReport.value))
const inflowAlertSummary = computed(() => buildAlertSummary(distInflowReport.value))
const congestionAlertSummary = computed(() => buildAlertSummary(distCongestionReport.value))

function formatMaxContext(point) {
  if (!point?.row) return '-'
  const id = point.row.layoutId ? `${point.row.layoutId}` : '-'
  const time = point.row.snapshotTime ? dashboard.formatTimestamp(point.row.snapshotTime, dashboard.locale) : ''
  return time ? `${id} · ${time}` : id
}

const insightItems = computed(() => {
  const isKo = dashboard.locale === 'ko'
  const delayTone = strongestTone(distDelayReport.value.counts)
  const inflowTone = strongestTone(distInflowReport.value.counts)
  const congestionTone = strongestTone(distCongestionReport.value.counts)

  return [
    {
      key: 'delay',
      title: dashboard.text.shippingDelay,
      tone: delayTone,
      lines: [
        formatAlertLine(delayAlertSummary.value),
        isKo
          ? `평균: ${dashboard.formatMinutes(distAverageDelay.value)} · 최대: ${dashboard.formatMinutes(distMaxDelay.value?.value)} (${formatMaxContext(distMaxDelay.value)})`
          : `Avg: ${dashboard.formatMinutes(distAverageDelay.value)} · Max: ${dashboard.formatMinutes(distMaxDelay.value?.value)} (${formatMaxContext(distMaxDelay.value)})`,
      ],
    },
    {
      key: 'inflow',
      title: dashboard.text.demandPressure,
      tone: inflowTone,
      lines: [
        formatAlertLine(inflowAlertSummary.value),
        isKo
          ? `피크(15m): ${dashboard.formatOrders(distPeakInflow.value)} · 최대: ${dashboard.formatOrders(distMaxInflow.value?.value)} (${formatMaxContext(distMaxInflow.value)})`
          : `Peak (15m): ${dashboard.formatOrders(distPeakInflow.value)} · Max: ${dashboard.formatOrders(distMaxInflow.value?.value)} (${formatMaxContext(distMaxInflow.value)})`,
      ],
    },
    {
      key: 'congestion',
      title: dashboard.text.flowFriction,
      tone: congestionTone,
      lines: [
        formatAlertLine(congestionAlertSummary.value),
        isKo
          ? `평균: ${dashboard.formatNumber(distAverageCongestion.value, 1)} · 최대: ${dashboard.formatNumber(distMaxCongestion.value?.value, 1)} (${formatMaxContext(distMaxCongestion.value)})`
          : `Avg: ${dashboard.formatNumber(distAverageCongestion.value, 1)} · Max: ${dashboard.formatNumber(distMaxCongestion.value?.value, 1)} (${formatMaxContext(distMaxCongestion.value)})`,
      ],
    },
  ]
})

const overviewRiskFactories = computed(() => {
  if (!overviewFactories.value.length) return []

  const maxCongestion = overviewMaxCongestionScore.value
  return overviewFactories.value
    .map((row) => {
      const delayTone = dashboard.getDelayStatus(row.avgDelayMinutesNext30m, dashboard.locale).tone
      const inflowTone = dashboard.getInflowStatus(row.orderInflow15m, dashboard.locale).tone
      const congestionTone = dashboard.getCongestionTone(row.congestionScore, maxCongestion)
      const score = toneWeight(delayTone) + toneWeight(inflowTone) + toneWeight(congestionTone)
      return {
        layoutId: row.layoutId,
        delayTone,
        inflowTone,
        congestionTone,
        score,
      }
    })
    .filter((entry) => Boolean(entry.layoutId) && entry.score > 0)
    .sort((a, b) => b.score - a.score || String(a.layoutId).localeCompare(String(b.layoutId)))
    .slice(0, 6)
})
</script>

<template>
  <section class="page">
    <header class="hero">
      <p class="hero-kicker">{{ dashboard.text.appTagline }}</p>
      <h1>{{ dashboard.locale === 'ko' ? '전체 분포 요약' : 'Distribution Summary' }}</h1>
      <p class="hero-subtitle">
        {{ dashboard.locale === 'ko'
          ? '내가 권한을 가진 공장들의 상태 분포를 확인하세요.'
          : 'Check status distributions across factories you have access to.' }}
      </p>
    </header>

    <section class="grid lower-grid">
      <article class="card dist-card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.text.distribution }}</p>
          <div class="card-actions">
            <span class="scope-chip">{{ accessScopeLabel }}</span>
            <InfoHint
              :title="dashboard.locale === 'ko' ? '분포 해석' : 'How to read'"
              :body="distributionNote"
              :points="distributionHintPoints"
              align="right"
            />
          </div>
        </div>

        <div v-if="isOverviewLoading && !overviewFactories.length" class="dist-loading-block">
          <p>
            {{ overviewSlow
              ? (dashboard.locale === 'ko' ? '서버 응답이 지연되고 있어요. 잠시만 기다려주세요…' : 'Server response is slow — hang tight…')
              : (dashboard.locale === 'ko' ? '공장 분포를 불러오는 중입니다...' : 'Loading factory distribution...') }}
          </p>
          <div class="dist-progress">
            <span>{{ overviewProgress }}%</span>
            <progress :value="overviewProgress" max="100"></progress>
          </div>
        </div>

        <div v-else-if="overviewErrorMessage && !overviewFactories.length" class="dist-state is-error">
          <p>{{ overviewErrorMessage }}</p>
          <button type="button" class="retry-button" @click="loadFactoriesOverview({ force: true })">
            {{ dashboard.locale === 'ko' ? '다시 시도' : 'Retry' }}
          </button>
        </div>

        <template v-else>
          <div v-if="overviewErrorMessage" class="dist-state is-error">
            <p>{{ overviewErrorMessage }}</p>
          </div>

          <div class="dist-grid">
            <div class="dist-item">
              <DonutChart :segments="distDelaySegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.shippingDelay }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
              <div class="legend">
                <div v-for="segment in distDelaySegments" :key="segment.key" class="legend-item">
                  <div class="legend-left">
                    <span class="legend-dot" :style="{ background: segment.color }" aria-hidden="true"></span>
                    <span class="legend-label">{{ segment.label }}</span>
                  </div>
                  <strong class="legend-value">
                    {{ dashboard.formatNumber(segment.value) }}
                    <span class="legend-share">({{ segment.share }}%)</span>
                  </strong>
                </div>
                <p v-if="!distDelaySegments.length" class="legend-empty">{{ dashboard.text.noData }}</p>
              </div>
            </div>
            <div class="dist-item">
              <DonutChart :segments="distInflowSegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.demandPressure }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
              <div class="legend">
                <div v-for="segment in distInflowSegments" :key="segment.key" class="legend-item">
                  <div class="legend-left">
                    <span class="legend-dot" :style="{ background: segment.color }" aria-hidden="true"></span>
                    <span class="legend-label">{{ segment.label }}</span>
                  </div>
                  <strong class="legend-value">
                    {{ dashboard.formatNumber(segment.value) }}
                    <span class="legend-share">({{ segment.share }}%)</span>
                  </strong>
                </div>
                <p v-if="!distInflowSegments.length" class="legend-empty">{{ dashboard.text.noData }}</p>
              </div>
            </div>
            <div class="dist-item">
              <DonutChart :segments="distCongestionSegments" :size="9.8" :hole="5.2">
                <div class="donut-center">
                  <strong>{{ dashboard.text.flowFriction }}</strong>
                  <span>{{ distributionScopeLabel }}</span>
                </div>
              </DonutChart>
              <div class="legend">
                <div v-for="segment in distCongestionSegments" :key="segment.key" class="legend-item">
                  <div class="legend-left">
                    <span class="legend-dot" :style="{ background: segment.color }" aria-hidden="true"></span>
                    <span class="legend-label">{{ segment.label }}</span>
                  </div>
                  <strong class="legend-value">
                    {{ dashboard.formatNumber(segment.value) }}
                    <span class="legend-share">({{ segment.share }}%)</span>
                  </strong>
                </div>
                <p v-if="!distCongestionSegments.length" class="legend-empty">{{ dashboard.text.noData }}</p>
              </div>
            </div>
          </div>

          <p class="dist-note">{{ distributionNote }}</p>
        </template>
      </article>

      <article class="card insight-card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '핵심 요약' : 'Highlights' }}</p>
        </div>

        <div v-if="isOverviewLoading && !overviewFactories.length" class="dist-loading-block">
          <p>
            {{ overviewSlow
              ? (dashboard.locale === 'ko' ? '요약 정보를 불러오는 중(지연됨)…' : 'Loading highlights (slow)…')
              : (dashboard.locale === 'ko' ? '요약 정보를 불러오는 중…' : 'Loading highlights…') }}
          </p>
          <div class="dist-progress">
            <span>{{ overviewProgress }}%</span>
            <progress :value="overviewProgress" max="100"></progress>
          </div>
        </div>

        <div v-else-if="overviewErrorMessage && !overviewFactories.length" class="dist-state is-error">
          <p>{{ overviewErrorMessage }}</p>
          <button type="button" class="retry-button" @click="loadFactoriesOverview({ force: true })">
            {{ dashboard.locale === 'ko' ? '다시 시도' : 'Retry' }}
          </button>
        </div>

        <template v-else>
          <div class="meta-grid">
            <div v-for="item in highlightMetaItems" :key="item.label" class="meta-chip">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <ul class="insight-list">
            <li v-for="item in insightItems" :key="item.key" class="insight-item" :class="toneClass(item.tone)">
              <div class="insight-title">
                <strong>{{ item.title }}</strong>
                <span class="insight-badge" :class="toneClass(item.tone)">{{ toneBadgeLabel(item.tone) }}</span>
              </div>
              <div class="insight-lines">
                <p v-for="line in item.lines" :key="line">{{ line }}</p>
              </div>
            </li>
          </ul>

          <div v-if="overviewRiskFactories.length" class="risk-shell">
            <p class="risk-kicker">{{ dashboard.locale === 'ko' ? '위험 공장 TOP' : 'Top risk factories' }}</p>
            <ul class="risk-list">
              <li v-for="(factory, index) in overviewRiskFactories" :key="factory.layoutId" class="risk-item">
                <div class="risk-left">
                  <span class="risk-rank">{{ index + 1 }}</span>
                  <strong>{{ factory.layoutId }}</strong>
                  <span class="risk-score">{{ factory.score }}/9</span>
                </div>
                <div class="risk-dots" aria-label="risk tones">
                  <span class="risk-dot" :class="toneClass(factory.delayTone)" :title="dashboard.text.shippingDelay" aria-hidden="true"></span>
                  <span class="risk-dot" :class="toneClass(factory.inflowTone)" :title="dashboard.text.demandPressure" aria-hidden="true"></span>
                  <span class="risk-dot" :class="toneClass(factory.congestionTone)" :title="dashboard.text.flowFriction" aria-hidden="true"></span>
                </div>
              </li>
            </ul>
          </div>
        </template>
      </article>
    </section>
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

.grid {
  display: grid;
  gap: 1rem;
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

.card-actions {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  justify-content: flex-end;
}

.dist-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
}

.dist-item {
  display: grid;
  gap: 0.9rem;
  justify-items: center;
  padding: 0.15rem 0;
}

.scope-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.75rem;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-weight: 900;
  font-size: 0.82rem;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.dist-loading-block {
  display: grid;
  gap: 1rem;
  padding: 1.25rem 0;
}

.dist-loading-block p {
  margin: 0;
  font-weight: 700;
  color: #334155;
}

.dist-progress {
  display: grid;
  gap: 0.5rem;
}

.dist-progress span {
  font-size: 0.95rem;
  color: #475569;
  font-weight: 800;
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

.dist-state {
  margin-top: 0.35rem;
  color: #64748b;
  font-weight: 800;
  font-size: 0.92rem;
  display: grid;
  gap: 0.75rem;
}

.dist-state.is-error {
  color: #b91c1c;
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

.legend {
  width: min(100%, 14.5rem);
  display: grid;
  gap: 0.5rem;
  padding: 0.75rem 0.85rem;
  border-radius: 1.2rem;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
}

.legend-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.legend-dot {
  width: 0.6rem;
  height: 0.6rem;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  color: #475569;
  font-weight: 900;
  font-size: 0.82rem;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.legend-value {
  color: #0f172a;
  font-weight: 900;
  font-size: 0.82rem;
  display: inline-flex;
  gap: 0.3rem;
  align-items: baseline;
  white-space: nowrap;
}

.legend-share {
  color: #64748b;
  font-weight: 900;
  font-size: 0.72rem;
}

.legend-empty {
  margin: 0;
  color: #64748b;
  font-weight: 800;
  font-size: 0.9rem;
  text-align: center;
}

.dist-note {
  color: #64748b;
  font-size: 0.9rem;
  line-height: 1.55;
  margin: 0;
}

.retry-button {
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1rem;
  background: #0f172a;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
  width: fit-content;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.meta-chip {
  padding: 0.75rem 0.9rem;
  border-radius: 1.25rem;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.meta-chip span {
  display: block;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.meta-chip strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.02rem;
  font-weight: 900;
  color: #0f172a;
}

.insight-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.insight-item {
  border-radius: 1.2rem;
  padding: 0.85rem 0.9rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.9);
  display: grid;
  gap: 0.5rem;
}

.insight-item.is-watch {
  border-color: rgba(245, 158, 11, 0.24);
}

.insight-item.is-warning {
  border-color: rgba(251, 146, 60, 0.32);
}

.insight-item.is-critical {
  border-color: rgba(239, 68, 68, 0.32);
}

.insight-title {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.insight-title strong {
  font-weight: 900;
  color: #0f172a;
}

.insight-badge {
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: #64748b;
}

.insight-badge.is-good {
  background: rgba(15, 118, 110, 0.14);
  color: #0f766e;
}

.insight-badge.is-watch {
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
}

.insight-badge.is-warning {
  background: rgba(251, 146, 60, 0.18);
  color: #c2410c;
}

.insight-badge.is-critical {
  background: rgba(239, 68, 68, 0.16);
  color: #b91c1c;
}

.insight-badge.is-muted {
  background: rgba(148, 163, 184, 0.16);
  color: #64748b;
}

.insight-lines p {
  margin: 0;
  color: #475569;
  font-size: 0.9rem;
  line-height: 1.55;
  font-weight: 700;
}

.risk-shell {
  display: grid;
  gap: 0.6rem;
  padding-top: 0.15rem;
}

.risk-kicker {
  margin: 0;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.risk-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.55rem;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  padding: 0.7rem 0.8rem;
  border-radius: 1.2rem;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.risk-left {
  display: inline-flex;
  gap: 0.55rem;
  align-items: center;
  min-width: 0;
}

.risk-rank {
  width: 1.35rem;
  height: 1.35rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
  font-weight: 900;
  font-size: 0.78rem;
  flex-shrink: 0;
}

.risk-item strong {
  font-weight: 900;
  color: #0f172a;
}

.risk-score {
  color: #64748b;
  font-weight: 900;
  font-size: 0.78rem;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.risk-dots {
  display: inline-flex;
  gap: 0.35rem;
  align-items: center;
}

.risk-dot {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 50%;
  background: #94a3b8;
}

.risk-dot.is-good {
  background: #0f766e;
}

.risk-dot.is-watch {
  background: #f59e0b;
}

.risk-dot.is-warning {
  background: #fb923c;
}

.risk-dot.is-critical {
  background: #ef4444;
}

.risk-dot.is-muted {
  background: #94a3b8;
}

@media (max-width: 1200px) {
  .lower-grid {
    grid-template-columns: 1fr;
  }

  .dist-grid {
    grid-template-columns: 1fr;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-wrap: wrap;
  }
}
</style>
