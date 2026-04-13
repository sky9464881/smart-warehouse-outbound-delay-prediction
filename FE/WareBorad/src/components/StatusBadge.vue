<script setup>
defineProps({
  label: {
    type: String,
    required: true,
  },
  tone: {
    type: String,
    default: 'muted',
  },
})
</script>

<template>
  <span class="status-badge" :class="`is-${tone}`">{{ label }}</span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  min-height: 2rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.is-good {
  background: rgba(52, 211, 153, 0.16);
  color: #0f766e;
}

.is-watch {
  background: rgba(250, 204, 21, 0.18);
  color: #a16207;
}

.is-warning {
  background: rgba(251, 146, 60, 0.18);
  color: #c2410c;
}

.is-critical {
  background: rgba(248, 113, 113, 0.18);
  color: #b91c1c;
}

.is-muted {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}
</style>
