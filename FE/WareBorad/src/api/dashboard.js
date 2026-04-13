const API_BASE = '/api/v1/factories'

async function requestJson(url) {
  const response = await fetch(url)
  const payload = await response.json().catch(() => null)

  if (!response.ok) {
    throw new Error(payload?.message || 'Failed to load dashboard data.')
  }

  return payload
}

export function getFactories() {
  return requestJson(API_BASE)
}

export function getFactoryInfo(factoryId) {
  return requestJson(`${API_BASE}/${factoryId}`)
}

export function getScenarios(factoryId) {
  return requestJson(`${API_BASE}/${factoryId}/scenarios`)
}

export function getDashboard(factoryId, scenarioId) {
  const base = `${API_BASE}/${factoryId}/scenarios/${scenarioId}/dashboard`

  return Promise.all([
    requestJson(`${base}/shipping-delay`),
    requestJson(`${base}/order-inflow`),
    requestJson(`${base}/robot-summary`),
    requestJson(`${base}/congestion`),
  ])
}
