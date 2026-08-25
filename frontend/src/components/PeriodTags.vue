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
  border: 1px solid transparent;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  letter-spacing: .3px;
  transition: border-color .2s, background-color .2s;
}
.period-morning {
  border-color: #b9ddc5;
  background-color: #edf8f1;
  color: #2f7650;
}
.period-noon {
  border-color: #ead4a7;
  background-color: #fff8e8;
  color: #9a6a1d;
}
.period-evening {
  border-color: #c9cfe1;
  background-color: #f0f2f8;
  color: #4e5877;
}
.period-deducted {
  background-color: #C0C4CC;
  color: #fff;
  text-decoration: line-through;
  box-shadow: none;
  opacity: 0.8;
}
.period-threshold-passed {
  border-color: #efb7b7;
  background-color: #fff1f1;
  color: #b74343;
}
</style>
