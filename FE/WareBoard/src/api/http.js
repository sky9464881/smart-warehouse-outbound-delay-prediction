const TOKEN_KEY = 'wb_token'
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim().replace(/\/$/, '')

function resolveUrl(url) {
  if (!API_BASE_URL) {
    return url
  }

  if (typeof url !== 'string') {
    return url
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  if (url.startsWith('/')) {
    return `${API_BASE_URL}${url}`
  }

  return `${API_BASE_URL}/${url}`
}

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

  let response
  try {
    response = await fetch(resolveUrl(url), { ...options, headers })
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw error
    }
    throw new Error('Cannot reach server. Check backend/proxy settings.')
  }

  const responseClone = response.clone()
  const payload = await response.json().catch(() => null)

  let message = payload?.message
  if (!message) {
    const text = await responseClone.text().catch(() => '')
    const trimmed = text.trim()
    if (trimmed) {
      message = trimmed.length > 200 ? `${trimmed.slice(0, 200)}...` : trimmed
    }
  }

  if (response.status === 401) {
    setAuthToken('')
    window.dispatchEvent(new Event('wb:unauthorized'))
  }

  if (!response.ok) {
    throw new Error(message || `Request failed (${response.status}).`)
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
