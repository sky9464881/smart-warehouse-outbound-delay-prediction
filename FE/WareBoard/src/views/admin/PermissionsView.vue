<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useAuthStore } from '@/stores/auth'
import { listUsers, updateUserFactories } from '@/api/admin'

const dashboard = useDashboardStore()
const auth = useAuthStore()

const users = ref([])
const selectedUserId = ref(null)
const selectedFactoryIds = ref([])
const factorySearch = ref('')

const isLoading = ref(false)
const isSaving = ref(false)
const message = ref('')
const errorMessage = ref('')

const factoryOptions = computed(() => dashboard.factories.map((factory) => factory.layoutId))
const filteredFactoryOptions = computed(() => {
  const query = factorySearch.value.trim().toLowerCase()
  if (!query) return factoryOptions.value
  return factoryOptions.value.filter((factoryId) => factoryId.toLowerCase().includes(query))
})

const sortedSelectedFactoryIds = computed(() =>
  [...selectedFactoryIds.value].sort((left, right) => left.localeCompare(right)),
)

const selectedFactoryCountLabel = computed(() => {
  const count = selectedFactoryIds.value.length
  return dashboard.locale === 'ko' ? `선택 ${count}개` : `${count} selected`
})

const selectableUsers = computed(() => users.value.filter((user) => user.role !== 'GLOBAL_ADMIN'))
const selectedUser = computed(() => selectableUsers.value.find((user) => user.id === selectedUserId.value) || null)

watch(selectedUser, (user) => {
  selectedFactoryIds.value = user ? [...(user.factoryIds || [])] : []
  factorySearch.value = ''
  message.value = ''
  errorMessage.value = ''
})

function selectAllFactories() {
  selectedFactoryIds.value = [...factoryOptions.value]
}

function clearAllFactories() {
  selectedFactoryIds.value = []
}

function selectFilteredFactories() {
  const next = new Set(selectedFactoryIds.value)
  for (const factoryId of filteredFactoryOptions.value) {
    next.add(factoryId)
  }
  selectedFactoryIds.value = [...next]
}

function clearFilteredFactories() {
  const toRemove = new Set(filteredFactoryOptions.value)
  selectedFactoryIds.value = selectedFactoryIds.value.filter((factoryId) => !toRemove.has(factoryId))
}

function removeFactory(factoryId) {
  selectedFactoryIds.value = selectedFactoryIds.value.filter((value) => value !== factoryId)
}

async function load() {
  isLoading.value = true
  errorMessage.value = ''
  message.value = ''

  try {
    if (!dashboard.factories.length) {
      await dashboard.bootstrap()
    }

    users.value = await listUsers()
    if (!selectedUserId.value) {
      selectedUserId.value = selectableUsers.value[0]?.id ?? null
    }
  } catch (error) {
    errorMessage.value = error.message || 'Failed to load users.'
  } finally {
    isLoading.value = false
  }
}

async function save() {
  if (!selectedUser.value) return

  isSaving.value = true
  message.value = ''
  errorMessage.value = ''

  try {
    const updated = await updateUserFactories(selectedUser.value.id, selectedFactoryIds.value)
    users.value = users.value.map((user) => (user.id === updated.id ? { ...user, ...updated } : user))
    message.value = '권한이 저장되었습니다.'
  } catch (error) {
    errorMessage.value = error.message || 'Failed to save permissions.'
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  if (auth.isGlobalAdmin) {
    load()
  }
})
</script>

<template>
  <section class="page">
    <header class="header">
      <div>
        <p class="kicker">{{ dashboard.text.nav.permissions }}</p>
        <h1>{{ dashboard.locale === 'ko' ? '공장 접근 권한 설정' : 'Factory access control' }}</h1>
        <p class="subtitle">
          {{ dashboard.locale === 'ko'
            ? '공장별 관리자가 모니터링할 수 있는 공장을 선택하고 저장하세요.'
            : 'Select which factories each manager can monitor, then save.' }}
        </p>
      </div>
    </header>

    <section class="grid">
      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '관리자 목록' : 'Managers' }}</p>
        </div>

        <div v-if="isLoading" class="state">{{ dashboard.text.loading }}</div>
        <div v-else-if="errorMessage" class="state is-error">{{ errorMessage }}</div>
        <div v-else-if="!selectableUsers.length" class="state">
          {{ dashboard.locale === 'ko' ? '공장별 관리자 계정이 없습니다.' : 'No factory admin users yet.' }}
        </div>
        <ul v-else class="user-list">
          <li v-for="user in selectableUsers" :key="user.id">
            <button
              type="button"
              class="user-item"
              :class="{ 'is-active': selectedUserId === user.id }"
              @click="selectedUserId = user.id"
            >
              <div class="user-main">
                <strong>{{ user.displayName }}</strong>
                <span>{{ user.email }}</span>
              </div>
              <span class="user-meta">{{ user.factoryIds?.length ?? 0 }} factories</span>
            </button>
          </li>
        </ul>
      </article>

      <article class="card">
        <div class="card-top">
          <p class="card-kicker">{{ dashboard.locale === 'ko' ? '공장 선택' : 'Factories' }}</p>
          <div class="card-actions">
            <span v-if="selectedUser" class="count-pill">{{ selectedFactoryCountLabel }}</span>
            <button class="action" type="button" :disabled="!selectedUser || isSaving" @click="save">
              {{ isSaving ? (dashboard.locale === 'ko' ? '저장 중...' : 'Saving...') : (dashboard.locale === 'ko' ? '저장' : 'Save') }}
            </button>
          </div>
        </div>

        <p v-if="selectedUser" class="hint">
          <strong>{{ selectedUser.displayName }}</strong>
          <span>{{ dashboard.locale === 'ko' ? '에게 허용할 공장을 선택하세요.' : 'Select allowed factories.' }}</span>
        </p>

        <div v-if="selectedUser" class="controls" aria-label="factory controls">
          <div class="control-row">
            <label class="search-field">
              <span class="search-label">{{ dashboard.locale === 'ko' ? '검색' : 'Search' }}</span>
              <input
                v-model.trim="factorySearch"
                type="search"
                :placeholder="dashboard.locale === 'ko' ? '공장 ID 검색 (예: WH_001)' : 'Search factory IDs (e.g., WH_001)'"
              />
            </label>

            <div class="bulk-actions" role="group" :aria-label="dashboard.locale === 'ko' ? '빠른 선택' : 'Quick selection'">
              <button type="button" class="bulk-button" :disabled="!factoryOptions.length" @click="selectAllFactories">
                {{ dashboard.locale === 'ko' ? '전체 선택' : 'Select all' }}
              </button>
              <button type="button" class="bulk-button" :disabled="!selectedFactoryIds.length" @click="clearAllFactories">
                {{ dashboard.locale === 'ko' ? '전체 해제' : 'Clear all' }}
              </button>
              <button type="button" class="bulk-button" :disabled="!filteredFactoryOptions.length" @click="selectFilteredFactories">
                {{ dashboard.locale === 'ko' ? '검색 선택' : 'Select shown' }}
              </button>
              <button
                type="button"
                class="bulk-button"
                :disabled="!filteredFactoryOptions.length || !selectedFactoryIds.length"
                @click="clearFilteredFactories"
              >
                {{ dashboard.locale === 'ko' ? '검색 해제' : 'Clear shown' }}
              </button>
            </div>
          </div>

          <div
            v-if="sortedSelectedFactoryIds.length"
            class="selected-cloud"
            role="list"
            :aria-label="dashboard.locale === 'ko' ? '선택된 공장' : 'Selected factories'"
          >
            <button
              v-for="factoryId in sortedSelectedFactoryIds"
              :key="factoryId"
              type="button"
              class="selected-chip"
              role="listitem"
              @click="removeFactory(factoryId)"
              :title="dashboard.locale === 'ko' ? '클릭해서 제거' : 'Click to remove'"
            >
              <span>{{ factoryId }}</span>
              <span class="chip-x" aria-hidden="true">×</span>
            </button>
          </div>
        </div>

        <div v-if="message" class="state is-success">{{ message }}</div>
        <div v-if="errorMessage && !isLoading" class="state is-error">{{ errorMessage }}</div>

        <div v-if="!selectedUser" class="state">
          {{ dashboard.locale === 'ko' ? '왼쪽에서 관리자를 선택하세요.' : 'Select a manager on the left.' }}
        </div>

        <div v-else-if="!factoryOptions.length" class="state">
          {{ dashboard.locale === 'ko' ? '공장 데이터가 없습니다.' : 'No factories found.' }}
        </div>

        <div v-else-if="!filteredFactoryOptions.length" class="state">
          {{ dashboard.locale === 'ko' ? '검색 결과가 없습니다.' : 'No matching factories.' }}
        </div>

        <div v-else class="factory-grid" role="group" :aria-label="dashboard.text.factory">
          <label v-for="factoryId in filteredFactoryOptions" :key="factoryId" class="factory-item">
            <input v-model="selectedFactoryIds" type="checkbox" :value="factoryId" />
            <span>{{ factoryId }}</span>
          </label>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 1.35rem;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 1.2rem;
  align-items: flex-end;
}

.kicker {
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

h1 {
  font-family: 'Bahnschrift', 'Aptos Display', 'Trebuchet MS', sans-serif;
  font-size: 2.2rem;
  line-height: 1.05;
  font-weight: 900;
  color: #0f172a;
  margin-top: 0.35rem;
}

.subtitle {
  margin-top: 0.45rem;
  color: #475569;
  font-weight: 700;
}

.grid {
  display: grid;
  grid-template-columns: 0.95fr 1.4fr;
  gap: 1.1rem;
  align-items: start;
}

.card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
  border-radius: 1.7rem;
  padding: 1.15rem 1.2rem;
  display: grid;
  gap: 0.9rem;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.card-kicker {
  color: #1d4ed8;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 900;
}

.action {
  border: 0;
  border-radius: 999px;
  padding: 0.55rem 0.95rem;
  background: #111827;
  color: #fff;
  font-weight: 900;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
}

.action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.card-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  justify-content: flex-end;
}

.count-pill {
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #475569;
  font-weight: 900;
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  white-space: nowrap;
}

.controls {
  display: grid;
  gap: 0.75rem;
  padding: 0.9rem 0.95rem;
  border-radius: 1.35rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.84);
}

.control-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

.search-field {
  display: grid;
  gap: 0.4rem;
}

.search-label {
  color: #475569;
  font-weight: 900;
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.search-field input {
  min-height: 2.75rem;
  padding: 0.65rem 0.8rem;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.95);
  color: #0f172a;
}

.search-field input:focus {
  outline: none;
  border-color: rgba(37, 99, 235, 0.5);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.14);
}

.bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.bulk-button {
  border: 0;
  border-radius: 999px;
  padding: 0.5rem 0.75rem;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-weight: 900;
  cursor: pointer;
  letter-spacing: 0.02em;
}

.bulk-button:hover {
  background: rgba(15, 23, 42, 0.12);
}

.bulk-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.selected-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  max-height: 7.5rem;
  overflow: auto;
  padding-right: 0.25rem;
}

.selected-chip {
  border: 1px solid rgba(29, 78, 216, 0.2);
  background: rgba(29, 78, 216, 0.08);
  color: #0f172a;
  border-radius: 999px;
  padding: 0.45rem 0.7rem;
  font-weight: 900;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.selected-chip:hover {
  border-color: rgba(29, 78, 216, 0.3);
  background: rgba(29, 78, 216, 0.12);
}

.chip-x {
  color: rgba(15, 23, 42, 0.6);
  font-weight: 900;
}

.state {
  border-radius: 1.25rem;
  border: 1px dashed rgba(15, 23, 42, 0.16);
  padding: 0.9rem 0.95rem;
  color: #64748b;
  font-weight: 800;
  font-size: 0.95rem;
}

.state.is-error {
  border-style: solid;
  border-color: rgba(239, 68, 68, 0.25);
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.06);
}

.state.is-success {
  border-style: solid;
  border-color: rgba(20, 184, 166, 0.25);
  color: #0f766e;
  background: rgba(20, 184, 166, 0.06);
}

.user-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.55rem;
}

.user-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.85rem;
  padding: 0.85rem 0.9rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.92);
  cursor: pointer;
  text-align: left;
  transition: transform 160ms ease, border-color 160ms ease, background-color 160ms ease;
}

.user-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.25);
}

.user-item.is-active {
  background: rgba(29, 78, 216, 0.08);
  border-color: rgba(29, 78, 216, 0.25);
}

.user-main {
  display: grid;
  gap: 0.2rem;
}

.user-main strong {
  font-weight: 900;
  color: #0f172a;
}

.user-main span {
  color: #64748b;
  font-weight: 800;
  font-size: 0.86rem;
}

.user-meta {
  color: #475569;
  font-weight: 900;
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  white-space: nowrap;
}

.hint {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
  align-items: baseline;
  color: #475569;
  font-weight: 800;
}

.hint strong {
  color: #0f172a;
  font-weight: 900;
}

.factory-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
  max-height: 22rem;
  overflow: auto;
  padding-right: 0.25rem;
}

.factory-item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.7rem 0.75rem;
  border-radius: 1.1rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  font-weight: 900;
  color: #0f172a;
}

.factory-item input {
  width: 1.05rem;
  height: 1.05rem;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .factory-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .factory-grid {
    grid-template-columns: 1fr;
  }
}
</style>
