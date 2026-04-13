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

export function formatTimestamp(value) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function formatTimeLabel(value) {
  if (!value) {
    return '-'
  }

  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

export function getDelayStatus(value) {
  if (value == null) {
    return { label: 'No data', tone: 'muted' }
  }

  if (value < 10) {
    return { label: 'Normal', tone: 'good' }
  }

  if (value <= 25) {
    return { label: 'Watch', tone: 'watch' }
  }

  if (value <= 45) {
    return { label: 'Warning', tone: 'warning' }
  }

  return { label: 'Critical', tone: 'critical' }
}

export function getInflowStatus(value) {
  if (value == null) {
    return { label: 'No data', tone: 'muted' }
  }

  if (value <= 30) {
    return { label: 'Low', tone: 'good' }
  }

  if (value <= 80) {
    return { label: 'Moderate', tone: 'watch' }
  }

  if (value <= 140) {
    return { label: 'High', tone: 'warning' }
  }

  return { label: 'Very high', tone: 'critical' }
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
