<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  body: {
    type: String,
    default: '',
  },
  points: {
    type: Array,
    default: () => [],
  },
  align: {
    type: String,
    default: 'left',
  },
})
</script>

<template>
  <details class="info-hint" :class="`align-${align}`">
    <summary class="info-trigger">
      <span aria-hidden="true">?</span>
      <span class="sr-only">{{ title }} information</span>
    </summary>

    <div class="info-card">
      <p class="info-title">{{ title }}</p>
      <p v-if="body" class="info-body">{{ body }}</p>

      <ul v-if="points.length" class="info-list">
        <li v-for="item in points" :key="item">{{ item }}</li>
      </ul>
    </div>
  </details>
</template>

<style scoped>
.info-hint {
  position: relative;
  display: inline-flex;
}

.info-trigger {
  display: inline-grid;
  place-items: center;
  width: 1.35rem;
  height: 1.35rem;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  cursor: pointer;
  list-style: none;
  font-size: 0.78rem;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.info-trigger::-webkit-details-marker {
  display: none;
}

.info-hint[open] .info-trigger {
  color: #0f172a;
  border-color: rgba(15, 23, 42, 0.2);
}

.info-card {
  position: absolute;
  top: calc(100% + 0.65rem);
  left: 0;
  z-index: 30;
  width: min(20rem, 70vw);
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.96);
  color: #334155;
  box-shadow: 0 22px 50px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(14px);
}

.align-right .info-card {
  right: 0;
  left: auto;
}

.info-title {
  color: #0f172a;
  font-size: 0.88rem;
  font-weight: 800;
}

.info-body {
  margin-top: 0.45rem;
  font-size: 0.83rem;
  line-height: 1.55;
}

.info-list {
  margin: 0.65rem 0 0;
  padding-left: 1rem;
  color: #475569;
  font-size: 0.8rem;
  line-height: 1.5;
}

.info-list li + li {
  margin-top: 0.35rem;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
