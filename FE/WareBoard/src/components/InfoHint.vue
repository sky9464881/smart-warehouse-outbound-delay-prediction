<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'

const props = defineProps({
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

const detailsRef = ref(null)
const triggerRef = ref(null)
const cardRef = ref(null)
const isOpen = ref(false)
const popoverTop = ref(0)
const popoverLeft = ref(0)
const popoverZIndex = ref(6000)

function nextZIndex() {
  if (typeof document === 'undefined') return 3001
  const root = document.documentElement
  const current = Number(root?.dataset?.infoHintZSeed ?? 6000)
  const safeCurrent = Number.isFinite(current) ? current : 6000
  const next = safeCurrent + 1
  root.dataset.infoHintZSeed = String(next)
  return next
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function updatePosition() {
  const triggerEl = triggerRef.value
  const cardEl = cardRef.value
  if (!triggerEl || !cardEl) return

  const anchor = triggerEl.getBoundingClientRect()
  const cardBox = cardEl.getBoundingClientRect()
  const padding = 10
  const offset = 10

  let top = anchor.bottom + offset
  const bottomOverflow = top + cardBox.height > window.innerHeight - padding
  if (bottomOverflow) {
    const aboveTop = anchor.top - offset - cardBox.height
    top = aboveTop >= padding ? aboveTop : Math.max(padding, window.innerHeight - padding - cardBox.height)
  }

  const preferredLeft = props.align === 'right' ? anchor.right - cardBox.width : anchor.left
  const left = clamp(preferredLeft, padding, window.innerWidth - padding - cardBox.width)

  popoverTop.value = Math.round(top)
  popoverLeft.value = Math.round(left)
}

let listenersBound = false

function unbindListeners() {
  if (!listenersBound) return
  listenersBound = false
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', updatePosition, true)
  document.removeEventListener('pointerdown', handleOutsidePointerDown, true)
  document.removeEventListener('keydown', handleKeyDown, true)
}

function bindListeners() {
  if (listenersBound) return
  listenersBound = true
  window.addEventListener('resize', updatePosition)
  window.addEventListener('scroll', updatePosition, true)
  document.addEventListener('pointerdown', handleOutsidePointerDown, true)
  document.addEventListener('keydown', handleKeyDown, true)
}

function close() {
  const detailsEl = detailsRef.value
  if (detailsEl?.open) detailsEl.open = false
  isOpen.value = false
  unbindListeners()
}

function handleOutsidePointerDown(event) {
  const target = event.target
  const detailsEl = detailsRef.value
  const cardEl = cardRef.value
  if (detailsEl?.contains(target)) return
  if (cardEl?.contains(target)) return
  close()
}

function handleKeyDown(event) {
  if (!isOpen.value) return
  if (event.key !== 'Escape') return
  event.preventDefault()
  close()
  triggerRef.value?.focus?.()
}

async function handleToggle() {
  const detailsEl = detailsRef.value
  const nextOpen = Boolean(detailsEl?.open)
  if (!nextOpen) {
    close()
    return
  }

  isOpen.value = true
  popoverZIndex.value = nextZIndex()
  await nextTick()
  updatePosition()
  bindListeners()
  requestAnimationFrame(updatePosition)
}

onBeforeUnmount(() => {
  unbindListeners()
})
</script>

<template>
  <details ref="detailsRef" class="info-hint" @toggle="handleToggle">
    <summary ref="triggerRef" class="info-trigger">
      <span aria-hidden="true">?</span>
      <span class="sr-only">{{ props.title }} information</span>
    </summary>

    <Teleport to="body">
      <div
        v-if="isOpen"
        ref="cardRef"
        class="info-card"
        :style="{ top: `${popoverTop}px`, left: `${popoverLeft}px`, zIndex: popoverZIndex }"
        role="dialog"
        :aria-label="`${props.title} details`"
      >
        <p class="info-title">{{ props.title }}</p>
        <p v-if="props.body" class="info-body">{{ props.body }}</p>

        <ul v-if="props.points.length" class="info-list">
          <li v-for="item in props.points" :key="item">{{ item }}</li>
        </ul>
      </div>
    </Teleport>
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
  position: fixed;
  z-index: 6000;
  width: min(20rem, 70vw);
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.96);
  color: #334155;
  box-shadow: 0 22px 50px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(14px);
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
