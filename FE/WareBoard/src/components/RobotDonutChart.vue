<script setup>
import { computed } from 'vue'

const props = defineProps({
  segments: {
    type: Array,
    default: () => [],
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
  <div class="donut-shell">
    <div class="donut-chart" :style="{ background: gradient }">
      <div class="donut-hole">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.donut-shell {
  display: grid;
  place-items: center;
}

.donut-chart {
  display: grid;
  place-items: center;
  width: 15rem;
  height: 15rem;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.donut-hole {
  display: grid;
  place-items: center;
  width: 8rem;
  height: 8rem;
  border-radius: 50%;
  background: rgba(247, 247, 243, 0.94);
  text-align: center;
  box-shadow: 0 20px 35px rgba(30, 41, 59, 0.08);
}
</style>
