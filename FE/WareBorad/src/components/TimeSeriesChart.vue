<script setup>
import { computed } from 'vue'

const props = defineProps({
  labels: {
    type: Array,
    default: () => [],
  },
  displayLabels: {
    type: Array,
    default: () => [],
  },
  selectedLabel: {
    type: String,
    default: '',
  },
  series: {
    type: Array,
    default: () => [],
  },
})

const viewBoxWidth = 720
const viewBoxHeight = 240
const padding = {
  top: 16,
  right: 18,
  bottom: 22,
  left: 18,
}

const innerWidth = viewBoxWidth - padding.left - padding.right
const innerHeight = viewBoxHeight - padding.top - padding.bottom

const activeSeries = computed(() =>
  props.series.filter((entry) => entry.values.some((value) => value != null)),
)

const hasData = computed(() => activeSeries.value.length > 0 && props.labels.length > 0)

const maxValue = computed(() => {
  const values = activeSeries.value.flatMap((entry) => entry.values.filter((value) => value != null))
  const peak = Math.max(...values, 0)
  return peak === 0 ? 1 : peak * 1.1
})

const selectedIndex = computed(() => props.labels.indexOf(props.selectedLabel))

function getX(index) {
  if (props.labels.length <= 1) {
    return padding.left + innerWidth / 2
  }

  return padding.left + (innerWidth / (props.labels.length - 1)) * index
}

function getY(value) {
  return padding.top + innerHeight - (value / maxValue.value) * innerHeight
}

function buildPath(values) {
  let path = ''

  values.forEach((value, index) => {
    if (value == null) {
      return
    }

    const command = path ? 'L' : 'M'
    path += `${command} ${getX(index)} ${getY(value)} `
  })

  return path.trim()
}

const linePaths = computed(() =>
  activeSeries.value.map((entry) => ({
    key: entry.key,
    color: entry.color,
    path: buildPath(entry.values),
  })),
)

const selectedX = computed(() => {
  if (selectedIndex.value < 0) {
    return null
  }

  return getX(selectedIndex.value)
})

const selectedDots = computed(() =>
  activeSeries.value
    .map((entry) => {
      const value = entry.values[selectedIndex.value]

      if (value == null || selectedIndex.value < 0) {
        return null
      }

      return {
        key: entry.key,
        color: entry.color,
        x: getX(selectedIndex.value),
        y: getY(value),
      }
    })
    .filter(Boolean),
)

const tickLabels = computed(() => {
  if (!props.displayLabels.length) {
    return ['--', '--', '--']
  }

  const middleIndex = Math.floor((props.displayLabels.length - 1) / 2)
  return [
    props.displayLabels[0],
    props.displayLabels[middleIndex],
    props.displayLabels[props.displayLabels.length - 1],
  ]
})
</script>

<template>
  <div class="chart-shell">
    <svg
      v-if="hasData"
      class="chart-svg"
      :viewBox="`0 0 ${viewBoxWidth} ${viewBoxHeight}`"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <line
        class="grid-line"
        :x1="padding.left"
        :x2="viewBoxWidth - padding.right"
        :y1="padding.top"
        :y2="padding.top"
      />
      <line
        class="grid-line"
        :x1="padding.left"
        :x2="viewBoxWidth - padding.right"
        :y1="padding.top + innerHeight / 2"
        :y2="padding.top + innerHeight / 2"
      />
      <line
        class="grid-line"
        :x1="padding.left"
        :x2="viewBoxWidth - padding.right"
        :y1="padding.top + innerHeight"
        :y2="padding.top + innerHeight"
      />

      <line
        v-if="selectedX != null"
        class="selection-line"
        :x1="selectedX"
        :x2="selectedX"
        :y1="padding.top"
        :y2="padding.top + innerHeight"
      />

      <path
        v-for="entry in linePaths"
        :key="entry.key"
        class="series-line"
        :d="entry.path"
        :stroke="entry.color"
      />

      <circle
        v-for="dot in selectedDots"
        :key="dot.key"
        class="selection-dot"
        :cx="dot.x"
        :cy="dot.y"
        :fill="dot.color"
      />
    </svg>

    <div v-else class="chart-empty">No time-series data for this scenario.</div>

    <div class="axis-labels">
      <span>{{ tickLabels[0] }}</span>
      <span>{{ tickLabels[1] }}</span>
      <span>{{ tickLabels[2] }}</span>
    </div>

    <div v-if="activeSeries.length > 1" class="chart-legend">
      <span v-for="entry in activeSeries" :key="entry.key" class="legend-item">
        <span class="legend-dot" :style="{ backgroundColor: entry.color }"></span>
        {{ entry.label }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.chart-shell {
  display: grid;
  gap: 0.8rem;
}

.chart-svg {
  width: 100%;
  height: 14rem;
  overflow: visible;
}

.grid-line {
  stroke: rgba(148, 163, 184, 0.25);
  stroke-width: 1;
}

.selection-line {
  stroke: rgba(15, 23, 42, 0.2);
  stroke-width: 1.5;
  stroke-dasharray: 6 5;
}

.series-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.selection-dot {
  r: 5.5;
  stroke: rgba(255, 255, 255, 0.95);
  stroke-width: 3;
}

.axis-labels {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 600;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  color: #334155;
  font-size: 0.84rem;
  font-weight: 700;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.legend-dot {
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 999px;
}

.chart-empty {
  display: grid;
  place-items: center;
  min-height: 14rem;
  border-radius: 1.2rem;
  background: rgba(241, 245, 249, 0.8);
  color: #64748b;
  font-size: 0.92rem;
}
</style>
