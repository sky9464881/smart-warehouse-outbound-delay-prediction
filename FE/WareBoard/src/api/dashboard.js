import { requestJson } from '@/api/http'

const API_BASE = '/api/v1/factories'

export function getFactories() {
  return requestJson(API_BASE)
}

export function getFactoriesOverview(options = {}) {
  return requestJson(`${API_BASE}/overview`, options)
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
