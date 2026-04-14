<script setup>
import { computed, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import DashboardSidebar from '@/components/DashboardSidebar.vue'
import DashboardTopBar from '@/components/DashboardTopBar.vue'
import { useDashboardStore } from '@/stores/dashboard'

const dashboard = useDashboardStore()
const hasFactories = computed(() => dashboard.factories.length > 0)

onMounted(() => {
  dashboard.bootstrap()
})
</script>

<template>
  <div class="app-shell">
    <DashboardSidebar />

    <div class="main-shell">
      <DashboardTopBar />

      <section class="page-shell">
        <section v-if="dashboard.errorMessage" class="message-card is-error">
          <strong>{{ dashboard.locale === 'ko' ? '대시보드 데이터를 불러오지 못했습니다.' : 'Dashboard data could not be loaded.' }}</strong>
          <p>{{ dashboard.errorMessage }}</p>
          <button class="retry-button" type="button" @click="dashboard.bootstrap">{{ dashboard.text.reloadDashboard }}</button>
        </section>

        <section v-else-if="dashboard.isBooting" class="message-card">
          <p>{{ dashboard.text.preparingDashboard }}</p>
        </section>

        <section v-else-if="!hasFactories" class="message-card">
          <strong>{{ dashboard.locale === 'ko' ? '접근 가능한 공장이 없습니다.' : 'No factories assigned.' }}</strong>
          <p>
            {{ dashboard.locale === 'ko'
              ? '전체 관리자에게 공장 접근 권한을 요청하세요.'
              : 'Ask a global admin to grant factory access.' }}
          </p>
        </section>

        <RouterView v-else />
      </section>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 17.5rem minmax(0, 1fr);
  background:
    radial-gradient(circle at 12% 10%, rgba(37, 99, 235, 0.12), transparent 30%),
    radial-gradient(circle at 92% 0%, rgba(249, 115, 22, 0.14), transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #fffaf6 34%, #eef4ff 100%);
  color: #18212f;
  font-family: 'Aptos', 'Trebuchet MS', 'Segoe UI', sans-serif;
}

.main-shell {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
}

.page-shell {
  min-width: 0;
  padding: 1.5rem 1.6rem 2.4rem;
}

.message-card {
  padding: 1.2rem 1.25rem;
  border-radius: 1.6rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 20px 55px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
  display: grid;
  gap: 0.65rem;
}

.message-card strong {
  color: #0f172a;
  font-weight: 900;
}

.message-card p {
  color: #475569;
  font-weight: 700;
}

.message-card.is-error {
  border-color: rgba(239, 68, 68, 0.22);
  background: rgba(254, 242, 242, 0.92);
}

.retry-button {
  justify-self: start;
  border: 0;
  border-radius: 999px;
  padding: 0.65rem 1rem;
  background: #0f172a;
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .page-shell {
    padding: 1.1rem 1rem 2rem;
  }
}
</style>
