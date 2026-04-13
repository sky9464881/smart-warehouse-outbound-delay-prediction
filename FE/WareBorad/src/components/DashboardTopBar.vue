<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import InfoHint from '@/components/InfoHint.vue'

const dashboard = useDashboardStore()
const route = useRoute()

const pageTitle = computed(() => {
  const name = route.name
  if (name === 'delayFlow') return dashboard.text.nav.delayFlow
  if (name === 'demandPressure') return dashboard.text.nav.demandPressure
  if (name === 'capacity') return dashboard.text.nav.capacity
  if (name === 'flowFriction') return dashboard.text.nav.flowFriction
  return dashboard.text.nav.summary
})

async function handleFactoryChange(event) {
  await dashboard.setFactory(event.target.value)
}

async function handleScenarioChange(event) {
  await dashboard.setScenario(event.target.value)
}

function handleSnapshotChange(event) {
  dashboard.setSnapshot(event.target.value)
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-left">
      <div class="page-title">
        <p class="page-kicker">{{ dashboard.text.currentContext }}</p>
        <strong>{{ pageTitle }}</strong>
      </div>

      <div class="chip-row" aria-label="context chips">
        <span class="chip">
          <span class="chip-label">{{ dashboard.text.analysisWindow }}</span>
          <strong class="chip-value">{{ dashboard.timeWindowLabel }}</strong>
        </span>
        <span class="chip">
          <span class="chip-label">{{ dashboard.text.snapshotsInView }}</span>
          <strong class="chip-value">{{ dashboard.selectedSnapshotNumber }}</strong>
        </span>
      </div>
    </div>

    <div class="topbar-right">
      <label class="field">
        <span class="field-label">
          {{ dashboard.text.factory }}
          <InfoHint :title="dashboard.helpContent.factorySelector.title" :body="dashboard.helpContent.factorySelector.body" :points="dashboard.helpContent.factorySelector.points" />
        </span>
        <select :value="dashboard.selectedFactoryId" :disabled="dashboard.isBooting || dashboard.isFactoryLoading" @change="handleFactoryChange">
          <option v-for="factory in dashboard.factories" :key="factory.layoutId" :value="factory.layoutId">{{ factory.layoutId }}</option>
        </select>
      </label>

      <label class="field">
        <span class="field-label">
          {{ dashboard.text.scenario }}
          <InfoHint :title="dashboard.helpContent.scenarioSelector.title" :body="dashboard.helpContent.scenarioSelector.body" :points="dashboard.helpContent.scenarioSelector.points" />
        </span>
        <select :value="dashboard.selectedScenarioId" :disabled="dashboard.isBooting || dashboard.isFactoryLoading || !dashboard.scenarios.length" @change="handleScenarioChange">
          <option v-for="scenario in dashboard.scenarios" :key="scenario.scenarioId" :value="scenario.scenarioId">{{ scenario.scenarioId }}</option>
        </select>
      </label>

      <label class="field">
        <span class="field-label">
          {{ dashboard.text.snapshot }}
          <InfoHint :title="dashboard.helpContent.snapshot.title" :body="dashboard.helpContent.snapshot.body" :points="dashboard.helpContent.snapshot.points" align="right" />
        </span>
        <select :value="dashboard.selectedSnapshot" :disabled="dashboard.isDashboardLoading || !dashboard.snapshotOptions.length" @change="handleSnapshotChange">
          <option v-for="snapshot in dashboard.snapshotOptions" :key="snapshot" :value="snapshot">{{ dashboard.formatTimestamp(snapshot, dashboard.locale) }}</option>
        </select>
      </label>

      <div class="locale-toggle" role="group" :aria-label="dashboard.text.toggleLabel">
        <button type="button" class="locale-button" :class="{ 'is-active': dashboard.locale === 'en' }" @click="dashboard.locale = 'en'">
          {{ dashboard.text.english }}
        </button>
        <button type="button" class="locale-button" :class="{ 'is-active': dashboard.locale === 'ko' }" @click="dashboard.locale = 'ko'">
          {{ dashboard.text.korean }}
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  justify-content: space-between;
  gap: 1.2rem;
  padding: 1.05rem 1.25rem;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
}

.topbar-left {
  display: grid;
  align-content: start;
  gap: 0.65rem;
  min-width: 16rem;
}

.page-title {
  display: grid;
  gap: 0.15rem;
}

.page-kicker {
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 800;
}

.page-title strong {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 1.25rem;
  font-weight: 900;
  letter-spacing: 0.01em;
  color: #0f172a;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.chip {
  display: grid;
  gap: 0.1rem;
  padding: 0.5rem 0.75rem;
  border-radius: 1rem;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.06);
  min-width: 12rem;
}

.chip-label {
  color: #64748b;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.chip-value {
  color: #0f172a;
  font-size: 0.95rem;
  font-weight: 900;
}

.topbar-right {
  display: flex;
  gap: 0.85rem;
  align-items: flex-end;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field-label {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 800;
}

.field select {
  min-height: 2.75rem;
  min-width: 11.5rem;
  padding: 0.65rem 0.8rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.95);
  color: #0f172a;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.locale-toggle {
  display: inline-flex;
  padding: 0.22rem;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.9);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.locale-button {
  min-width: 3rem;
  padding: 0.5rem 0.8rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-weight: 900;
  cursor: pointer;
}

.locale-button.is-active {
  background: #111827;
  color: #fff;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.18);
}

@media (max-width: 1100px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .topbar-right {
    justify-content: flex-start;
  }

  .chip {
    min-width: 0;
    flex: 1 1 12rem;
  }
}
</style>

