<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useAuthStore } from '@/stores/auth'

const dashboard = useDashboardStore()
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const items = computed(() => ([
  { name: 'summary', label: dashboard.text.nav.summary, icon: 'grid' },
  { name: 'distributionSummary', label: dashboard.locale === 'ko' ? '전체 분포 요약' : 'Distribution Summary', icon: 'chart' },
  { name: 'delayFlow', label: dashboard.text.nav.delayFlow, icon: 'wave' },
  { name: 'demandPressure', label: dashboard.text.nav.demandPressure, icon: 'spark' },
  { name: 'capacity', label: dashboard.text.nav.capacity, icon: 'bot' },
  { name: 'flowFriction', label: dashboard.text.nav.flowFriction, icon: 'traffic' },
  ...(auth.isGlobalAdmin ? [{ name: 'adminPermissions', label: dashboard.text.nav.permissions, icon: 'shield' }] : []),
]))

function isActive(name) {
  if (route.name === name) return true
  if (name === 'summary' && route.matched.some((record) => record.name === 'summary')) return true
  return false
}

async function handleLogout() {
  await auth.logout()
  dashboard.reset()
  await router.push({ name: 'login' })
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true">WB</div>
      <div class="brand-copy">
        <strong class="brand-name">{{ dashboard.text.appName }}</strong>
        <span class="brand-tag">{{ dashboard.text.appTagline }}</span>
      </div>
    </div>

    <nav class="nav" :aria-label="dashboard.text.appName">
      <RouterLink
        v-for="item in items"
        :key="item.name"
        class="nav-link"
        :class="{ 'is-active': isActive(item.name) }"
        :to="{ name: item.name }"
      >
        <span class="nav-icon" aria-hidden="true">
          <svg v-if="item.icon === 'grid'" viewBox="0 0 20 20" fill="none">
            <path d="M3 3h6v6H3V3Zm8 0h6v6h-6V3ZM3 11h6v6H3v-6Zm8 0h6v6h-6v-6Z" fill="currentColor" />
          </svg>
          <svg v-else-if="item.icon === 'wave'" viewBox="0 0 20 20" fill="none">
            <path d="M2 12c2.2 0 2.2-6 4.4-6S8.6 16 11 16s2.2-10 4.6-10S18 12 18 12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else-if="item.icon === 'spark'" viewBox="0 0 20 20" fill="none">
            <path d="M10 2l1.2 4.2L16 7.5l-4.1 1.4L10 13l-1.9-4.1L4 7.5l4.8-1.3L10 2Z" fill="currentColor" opacity="0.92" />
            <path d="M3.2 12.2l.8 2.8 2.9.8-2.9.8-.8 2.8-.8-2.8-2.9-.8 2.9-.8.8-2.8Z" fill="currentColor" opacity="0.72" />
          </svg>
          <svg v-else-if="item.icon === 'bot'" viewBox="0 0 20 20" fill="none">
            <path d="M7 2h6v2H7V2Z" fill="currentColor" opacity="0.75" />
            <path d="M5 6.5A3.5 3.5 0 0 1 8.5 3h3A3.5 3.5 0 0 1 15 6.5V14a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V6.5Z" stroke="currentColor" stroke-width="1.5" />
            <path d="M7.6 9.2h.1M12.3 9.2h.1" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" />
            <path d="M7.5 12.8h5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
          <svg v-else-if="item.icon === 'traffic'" viewBox="0 0 20 20" fill="none">
            <path d="M3 11.5c0-4.4 3-8 7-8s7 3.6 7 8v2.2c0 1.5-1.2 2.8-2.8 2.8H5.8C4.2 16.5 3 15.2 3 13.7v-2.2Z" stroke="currentColor" stroke-width="1.6" />
            <path d="M6.2 12.3h7.6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            <path d="M5.9 9.2c1.2-1.3 2.7-2 4.1-2 1.5 0 3 .7 4.1 2" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
          <svg v-else-if="item.icon === 'shield'" viewBox="0 0 20 20" fill="none">
            <path
              d="M10 2.3 16 5v5.5c0 4-2.6 6.6-6 7.9-3.4-1.3-6-3.9-6-7.9V5l6-2.7Z"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linejoin="round"
            />
            <path d="M7.2 9.9 9 11.7l3.8-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else-if="item.icon === 'chart'" viewBox="0 0 20 20" fill="none">
            <path d="M3 3v14h14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M7 9l3-3 4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="7" cy="9" r="1" fill="currentColor" />
            <circle cx="10" cy="6" r="1" fill="currentColor" />
            <circle cx="14" cy="10" r="1" fill="currentColor" />
          </svg>
        </span>
        <span class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-foot">
      <div class="account">
        <div class="account-main">
          <strong class="account-name">{{ auth.displayName || auth.email }}</strong>
          <span class="account-meta">
            {{ auth.email }}
            <span class="role-pill">{{ auth.isGlobalAdmin ? 'GLOBAL ADMIN' : 'FACTORY ADMIN' }}</span>
          </span>
        </div>
        <button type="button" class="logout" @click="handleLogout">
          {{ dashboard.locale === 'ko' ? '로그아웃' : 'Logout' }}
        </button>
      </div>
      <p v-if="dashboard.selectedFactoryId" class="foot-line">
        <span>{{ dashboard.text.factory }}</span>
        <strong>{{ dashboard.selectedFactoryId }}</strong>
      </p>
      <p v-if="dashboard.selectedScenarioId" class="foot-line">
        <span>{{ dashboard.text.scenario }}</span>
        <strong>{{ dashboard.selectedScenarioId }}</strong>
      </p>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 1.2rem 1.05rem;
  background: linear-gradient(180deg, #0f167d 0%, #0a0f57 100%);
  color: rgba(255, 255, 255, 0.92);
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 1.1rem;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.85rem;
  align-items: center;
}

.brand-mark {
  width: 2.7rem;
  height: 2.7rem;
  border-radius: 0.95rem;
  display: grid;
  place-items: center;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 14px 35px rgba(0, 0, 0, 0.22);
}

.brand-copy {
  display: grid;
  gap: 0.1rem;
}

.brand-name {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 1.05rem;
  letter-spacing: 0.02em;
}

.brand-tag {
  color: rgba(255, 255, 255, 0.68);
  font-size: 0.82rem;
  font-weight: 600;
}

.nav {
  display: grid;
  gap: 0.25rem;
  align-content: start;
}

.nav-link {
  display: grid;
  grid-template-columns: 2.2rem 1fr;
  gap: 0.75rem;
  align-items: center;
  padding: 0.72rem 0.78rem;
  border-radius: 1rem;
  color: rgba(255, 255, 255, 0.78);
  transition: transform 160ms ease, background-color 160ms ease, color 160ms ease;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.95);
  transform: translateY(-1px);
}

.nav-link.is-active {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.98);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}

.nav-icon {
  width: 2.05rem;
  height: 2.05rem;
  border-radius: 0.85rem;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
}

.nav-link.is-active .nav-icon {
  background: rgba(255, 255, 255, 0.18);
}

.nav-icon svg {
  width: 1.15rem;
  height: 1.15rem;
}

.nav-label {
  font-weight: 800;
  letter-spacing: 0.01em;
}

.sidebar-foot {
  padding-top: 0.9rem;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  display: grid;
  gap: 0.4rem;
}

.account {
  display: grid;
  gap: 0.65rem;
  padding: 0.55rem 0.6rem 0.75rem;
  border-radius: 1.15rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.account-main {
  display: grid;
  gap: 0.25rem;
}

.account-name {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 900;
  font-size: 0.95rem;
  letter-spacing: 0.01em;
}

.account-meta {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 800;
  font-size: 0.8rem;
}

.role-pill {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.92);
  font-weight: 900;
  letter-spacing: 0.12em;
  font-size: 0.66rem;
}

.logout {
  border: 0;
  border-radius: 999px;
  padding: 0.55rem 0.75rem;
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.9);
  font-weight: 900;
  cursor: pointer;
}

.logout:hover {
  background: rgba(255, 255, 255, 0.18);
}

.foot-line {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.82rem;
  color: rgba(255, 255, 255, 0.72);
}

.foot-line strong {
  color: rgba(255, 255, 255, 0.95);
  font-weight: 900;
}

@media (max-width: 900px) {
  .sidebar {
    position: relative;
    height: auto;
    grid-template-rows: auto;
  }
}
</style>
