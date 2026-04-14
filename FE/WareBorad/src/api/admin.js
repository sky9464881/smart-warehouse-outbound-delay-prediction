import { putJson, requestJson } from '@/api/http'

const API_BASE = '/api/v1/admin'

export function listUsers() {
  return requestJson(`${API_BASE}/users`)
}

export function updateUserFactories(userId, factoryIds) {
  return putJson(`${API_BASE}/users/${userId}/factories`, { factoryIds })
}

