<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import DashboardSidebar from '@/components/DashboardSidebar.vue'
import DashboardTopBar from '@/components/DashboardTopBar.vue'
import { useDashboardStore } from '@/stores/dashboard'

const dashboard = useDashboardStore()

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
        <RouterView />
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

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .page-shell {
    padding: 1.1rem 1rem 2rem;
  }
}
</style>

