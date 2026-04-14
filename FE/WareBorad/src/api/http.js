const TOKEN_KEY = 'wb_token'

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setAuthToken(token) {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY)
    return
  }
  localStorage.setItem(TOKEN_KEY, token)
}

export async function requestJson(url, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = getAuthToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(url, { ...options, headers })
  const payload = await response.json().catch(() => null)

  if (response.status === 401) {
    setAuthToken('')
    window.dispatchEvent(new Event('wb:unauthorized'))
  }

  if (!response.ok) {
    throw new Error(payload?.message || 'Request failed.')
  }

  return payload
}

export function postJson(url, body) {
  return requestJson(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body != null ? JSON.stringify(body) : undefined,
  })
}

export function putJson(url, body) {
  return requestJson(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: body != null ? JSON.stringify(body) : undefined,
  })
}

