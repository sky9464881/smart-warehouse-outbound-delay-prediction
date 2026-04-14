import { postJson, requestJson } from '@/api/http'

const API_BASE = '/api/v1/auth'

export function login(credentials) {
  return postJson(`${API_BASE}/login`, credentials)
}

export function signup(payload) {
  return postJson(`${API_BASE}/signup`, payload)
}

export function me() {
  return requestJson(`${API_BASE}/me`)
}

export function logout() {
  return requestJson(`${API_BASE}/logout`, { method: 'POST' })
}

