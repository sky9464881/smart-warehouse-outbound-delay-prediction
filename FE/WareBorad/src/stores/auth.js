import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAuthToken, setAuthToken } from '@/api/http'
import { login as apiLogin, logout as apiLogout, me as apiMe, signup as apiSignup } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(getAuthToken())
  const user = ref(null)

  const isReady = ref(false)
  const isLoading = ref(false)
  const errorMessage = ref('')

  const isAuthenticated = computed(() => Boolean(token.value) && Boolean(user.value))
  const isGlobalAdmin = computed(() => user.value?.role === 'GLOBAL_ADMIN')

  const displayName = computed(() => user.value?.displayName || '')
  const email = computed(() => user.value?.email || '')
  const role = computed(() => user.value?.role || '')

  async function init() {
    if (isReady.value) return

    isLoading.value = true
    errorMessage.value = ''
    token.value = getAuthToken()

    try {
      if (token.value) {
        user.value = await apiMe()
      }
    } catch (error) {
      clearSession()
    } finally {
      isLoading.value = false
      isReady.value = true
    }
  }

  function clearSession() {
    token.value = ''
    user.value = null
    setAuthToken('')
  }

  async function login(credentials) {
    isLoading.value = true
    errorMessage.value = ''

    try {
      const payload = await apiLogin(credentials)
      token.value = payload.token || ''
      setAuthToken(token.value)
      user.value = payload.user || null
      return user.value
    } catch (error) {
      errorMessage.value = error.message || 'Login failed.'
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function signup(payload) {
    isLoading.value = true
    errorMessage.value = ''

    try {
      const response = await apiSignup(payload)
      token.value = response.token || ''
      setAuthToken(token.value)
      user.value = response.user || null
      return user.value
    } catch (error) {
      errorMessage.value = error.message || 'Signup failed.'
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function logout() {
    isLoading.value = true
    errorMessage.value = ''

    try {
      if (token.value) {
        await apiLogout().catch(() => null)
      }
    } finally {
      clearSession()
      isLoading.value = false
    }
  }

  async function refreshMe() {
    if (!token.value) return null
    user.value = await apiMe()
    return user.value
  }

  return {
    token,
    user,
    isReady,
    isLoading,
    errorMessage,
    isAuthenticated,
    isGlobalAdmin,
    displayName,
    email,
    role,
    init,
    login,
    signup,
    logout,
    clearSession,
    refreshMe,
  }
})

