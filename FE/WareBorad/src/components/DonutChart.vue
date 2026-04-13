<script setup>
import { computed } from 'vue'

const props = defineProps({
  segments: {
    type: Array,
    default: () => [],
  },
  size: {
    type: Number,
    default: 12,
  },
  hole: {
    type: Number,
    default: 6,
  },
  holeBackground: {
    type: String,
    default: 'rgba(247, 247, 243, 0.94)',
  },
})

const gradient = computed(() => {
  if (!props.segments.length) {
    return 'conic-gradient(#dbe5f0 0deg 360deg)'
  }

  let current = 0
  const stops = props.segments.map((segment) => {
    const start = current
    current += segment.angle
    return `${segment.color} ${start}deg ${current}deg`
  })

  return `conic-gradient(${stops.join(', ')})`
})
</script>

<template>
  <div
    class="donut-chart"
    :style="{
      background: gradient,
      '--donut-size': `${props.size}rem`,
      '--donut-hole': `${props.hole}rem`,
      '--donut-hole-bg': props.holeBackground,
    }"
  >
    <div class="donut-hole">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.donut-chart {
  display: grid;
  place-items: center;
  width: var(--donut-size);
  height: var(--donut-size);
  border-radius: 50%;
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.07),
    0 18px 45px rgba(15, 23, 42, 0.12);
}

.donut-hole {
  display: grid;
  place-items: center;
  width: var(--donut-hole);
  height: var(--donut-hole);
  border-radius: 50%;
  background: var(--donut-hole-bg);
  text-align: center;
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.08);
}
</style>

