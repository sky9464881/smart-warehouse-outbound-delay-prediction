export function formatNumber(value, maximumFractionDigits = 0) {
  if (value == null || Number.isNaN(Number(value))) {
    return '-'
  }

  return new Intl.NumberFormat('en-US', {
    maximumFractionDigits,
    minimumFractionDigits: maximumFractionDigits === 0 ? 0 : 0,
  }).format(value)
}

export function formatPercent(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return '-'
  }

  return `${formatNumber(value, 1)}%`
}

function resolveLocale(locale = 'en') {
  return locale === 'ko' ? 'ko-KR' : 'en-US'
}

export function formatTimestamp(value, locale = 'en') {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(resolveLocale(locale), {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function formatTimeLabel(value, locale = 'en') {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat(resolveLocale(locale), {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function getDelayStatus(value, locale = 'en') {
  const labels = locale === 'ko'
    ? {
        noData: '데이터 없음',
        normal: '정상',
        watch: '주의',
        warning: '경고',
        critical: '위험',
      }
    : {
        noData: 'No data',
        normal: 'Normal',
        watch: 'Watch',
        warning: 'Warning',
        critical: 'Critical',
      }

  if (value == null) {
    return { label: labels.noData, tone: 'muted' }
  }

  if (value < 10) {
    return { label: labels.normal, tone: 'good' }
  }

  if (value <= 25) {
    return { label: labels.watch, tone: 'watch' }
  }

  if (value <= 45) {
    return { label: labels.warning, tone: 'warning' }
  }

  return { label: labels.critical, tone: 'critical' }
}

export function getInflowStatus(value, locale = 'en') {
  const labels = locale === 'ko'
    ? {
        noData: '데이터 없음',
        low: '낮음',
        moderate: '보통',
        high: '높음',
        veryHigh: '매우 높음',
      }
    : {
        noData: 'No data',
        low: 'Low',
        moderate: 'Moderate',
        high: 'High',
        veryHigh: 'Very high',
      }

  if (value == null) {
    return { label: labels.noData, tone: 'muted' }
  }

  if (value <= 30) {
    return { label: labels.low, tone: 'good' }
  }

  if (value <= 80) {
    return { label: labels.moderate, tone: 'watch' }
  }

  if (value <= 140) {
    return { label: labels.high, tone: 'warning' }
  }

  return { label: labels.veryHigh, tone: 'critical' }
}

export function getCongestionTone(score, maxScore) {
  if (score == null) {
    return 'muted'
  }

  const ratio = maxScore > 0 ? score / maxScore : 0

  if (ratio < 0.35) {
    return 'good'
  }

  if (ratio < 0.65) {
    return 'watch'
  }

  if (ratio < 0.85) {
    return 'warning'
  }

  return 'critical'
}
