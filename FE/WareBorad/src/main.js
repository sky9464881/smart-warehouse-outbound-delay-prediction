import './assets/main.css'

import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import pinia from './stores/pinia'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)

app.use(pinia)
app.use(router)

const auth = useAuthStore(pinia)
auth.init()

window.addEventListener('wb:unauthorized', () => {
  const store = useAuthStore(pinia)
  store.clearSession()

  const current = router.currentRoute.value
  if (current?.name === 'login') return

  router.push({
    name: 'login',
    query: { redirect: current?.fullPath || '/' },
  })
})

app.mount('#app')
