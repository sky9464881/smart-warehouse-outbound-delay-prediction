<script setup>
import { computed, ref } from 'vue'

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
  valueFormatter: {
    type: Function,
    default: (value) => (value == null ? '-' : `${value}`),
  },
  tooltipLabelFormatter: {
    type: Function,
    default: (value) => value ?? '--',
  },
  emptyLabel: {
    type: String,
    default: 'No time-series data for this scenario.',
  },
})

const svgRef = ref(null)
const hoverIndex = ref(null)

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

const highlightIndex = computed(() => {
  if (hoverIndex.value != null) {
    return hoverIndex.value
  }

  if (selectedIndex.value >= 0) {
    return selectedIndex.value
  }

  return props.labels.length ? props.labels.length - 1 : -1
})

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

function resolveNearestIndex(clientX) {
  const svg = svgRef.value
  if (!svg || !props.labels.length) {
    return null
  }

  const rect = svg.getBoundingClientRect()
  const viewBoxX = ((clientX - rect.left) / rect.width) * viewBoxWidth

  let nearestIndex = 0
  let nearestDistance = Infinity

  props.labels.forEach((_, index) => {
    const distance = Math.abs(getX(index) - viewBoxX)
    if (distance < nearestDistance) {
      nearestDistance = distance
      nearestIndex = index
    }
  })

  return nearestIndex
}

function handlePointerMove(event) {
  hoverIndex.value = resolveNearestIndex(event.clientX)
}

function handleTouch(event) {
  const touch = event.touches?.[0]
  if (!touch) {
    return
  }

  hoverIndex.value = resolveNearestIndex(touch.clientX)
}

function clearHover() {
  hoverIndex.value = null
}

const linePaths = computed(() =>
  activeSeries.value.map((entry) => ({
    key: entry.key,
    color: entry.color,
    path: buildPath(entry.values),
  })),
)

const highlightX = computed(() => {
  if (highlightIndex.value < 0) {
    return null
  }

  return getX(highlightIndex.value)
})

const highlightDots = computed(() =>
  activeSeries.value
    .map((entry) => {
      const value = entry.values[highlightIndex.value]

      if (value == null || highlightIndex.value < 0) {
        return null
      }

      return {
        key: entry.key,
        color: entry.color,
        x: getX(highlightIndex.value),
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

const tooltipEntries = computed(() => {
  if (hoverIndex.value == null || hoverIndex.value < 0) {
    return []
  }

  return activeSeries.value
    .map((entry) => {
      const value = entry.values[hoverIndex.value]
      if (value == null) {
        return null
      }

      return {
        key: entry.key,
        label: entry.label,
        color: entry.color,
        value: props.valueFormatter(value, entry),
      }
    })
    .filter(Boolean)
})

const tooltipVisible = computed(() => tooltipEntries.value.length > 0 && hoverIndex.value != null)

const tooltipLabel = computed(() => {
  if (!tooltipVisible.value) {
    return ''
  }

  return props.tooltipLabelFormatter(props.labels[hoverIndex.value], hoverIndex.value)
})

const tooltipStyle = computed(() => {
  if (hoverIndex.value == null || highlightX.value == null) {
    return {}
  }

  const normalizedLeft = Math.min(84, Math.max(16, (highlightX.value / viewBoxWidth) * 100))

  return {
    left: `${normalizedLeft}%`,
  }
})
</script>

<template>
  <div class="chart-shell">
    <div class="chart-frame">
      <svg
        v-if="hasData"
        ref="svgRef"
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
          v-if="highlightX != null"
          class="selection-line"
          :x1="highlightX"
          :x2="highlightX"
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
          v-for="dot in highlightDots"
          :key="dot.key"
          class="selection-dot"
          :cx="dot.x"
          :cy="dot.y"
          :fill="dot.color"
        />

        <rect
          class="interaction-layer"
          :x="0"
          :y="0"
          :width="viewBoxWidth"
          :height="viewBoxHeight"
          @mousemove="handlePointerMove"
          @mouseleave="clearHover"
          @touchstart.prevent="handleTouch"
          @touchmove.prevent="handleTouch"
          @touchend="clearHover"
        />
      </svg>

      <div v-else class="chart-empty">{{ emptyLabel }}</div>

      <div v-if="tooltipVisible" class="chart-tooltip" :style="tooltipStyle">
        <p class="tooltip-label">{{ tooltipLabel }}</p>

        <div class="tooltip-values">
          <div v-for="entry in tooltipEntries" :key="entry.key" class="tooltip-row">
            <span class="tooltip-swatch" :style="{ backgroundColor: entry.color }"></span>
            <span class="tooltip-series">{{ entry.label }}</span>
            <strong class="tooltip-value">{{ entry.value }}</strong>
          </div>
        </div>
      </div>
    </div>

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

.chart-frame {
  position: relative;
  border-radius: 1.35rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.96)),
    linear-gradient(135deg, rgba(249, 115, 22, 0.06), rgba(59, 130, 246, 0.08));
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12);
}

.chart-svg {
  width: 100%;
  height: 14rem;
  overflow: visible;
}

.grid-line {
  stroke: rgba(148, 163, 184, 0.22);
  stroke-width: 1;
}

.selection-line {
  stroke: rgba(15, 23, 42, 0.26);
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

.interaction-layer {
  fill: transparent;
  cursor: crosshair;
}

.chart-tooltip {
  position: absolute;
  top: 0.85rem;
  z-index: 2;
  min-width: 11rem;
  padding: 0.85rem 0.95rem;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.92);
  color: white;
  box-shadow: 0 18px 30px rgba(15, 23, 42, 0.28);
  transform: translateX(-50%);
  pointer-events: none;
}

.tooltip-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.72);
}

.tooltip-values {
  display: grid;
  gap: 0.45rem;
  margin-top: 0.55rem;
}

.tooltip-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.55rem;
  align-items: center;
}

.tooltip-swatch,
.legend-dot {
  width: 0.72rem;
  height: 0.72rem;
  border-radius: 999px;
}

.tooltip-series {
  font-size: 0.8rem;
  font-weight: 600;
}

.tooltip-value {
  font-size: 0.8rem;
  font-weight: 800;
}

.axis-labels {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  color: #64748b;
  font-size: 0.82rem;
  font-weight: 700;
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
