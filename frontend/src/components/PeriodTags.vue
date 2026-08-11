<template>
  <span class="period-tags-wrapper">
    <span
      v-for="period in periods"
      :key="period"
      class="period-tag"
      :class="getTagClass(period)"
    >
      {{ getLabel(period) }}<template v-if="isDeducted(period)">✅</template>
    </span>
  </span>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  periods: { type: Array, default: () => [] },
  deducted: { type: Array, default: () => [] },
  frequencyCode: { type: String, default: '' }
})

const currentHour = ref(new Date().getHours())
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    currentHour.value = new Date().getHours()
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const periodLabelMap = {
  MORNING: '晨服',
  NOON: '午服',
  EVENING: '晚服'
}

const getLabel = (period) => periodLabelMap[period] || period

const isDeducted = (period) => {
  return props.deducted && props.deducted.includes(period)
}

const isThresholdPassed = (period) => {
  if (period === 'MORNING') return currentHour.value >= 9
  if (period === 'NOON') return currentHour.value >= 13
  if (period === 'EVENING') return currentHour.value >= 21
  return false
}

const getTagClass = (period) => {
  if (isDeducted(period)) {
    return 'period-deducted'
  }
  if (isThresholdPassed(period)) {
    return 'period-threshold-passed'
  }
  if (period === 'MORNING') return 'period-morning'
  if (period === 'NOON') return 'period-noon'
  if (period === 'EVENING') return 'period-evening'
  return ''
}
</script>

<style scoped>
.period-tags-wrapper {
  display: inline-flex;
  gap: 6px;
  flex-wrap: wrap;
  align-items: center;
}
.period-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: 1px;
  transition: all 0.3s;
}
.period-morning {
  background-color: #67C23A;
  color: #fff;
  box-shadow: 0 2px 4px rgba(103, 194, 58, 0.3);
}
.period-noon {
  background-color: #E6A23C;
  color: #fff;
  box-shadow: 0 2px 4px rgba(230, 162, 60, 0.3);
}
.period-evening {
  background-color: #1B2A4A;
  color: #fff;
  box-shadow: 0 2px 4px rgba(27, 42, 74, 0.3);
}
.period-deducted {
  background-color: #C0C4CC;
  color: #fff;
  text-decoration: line-through;
  box-shadow: none;
  opacity: 0.8;
}
.period-threshold-passed {
  background-color: #F56C6C;
  color: #fff;
  animation: blink 1s ease-in-out infinite;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.5);
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
