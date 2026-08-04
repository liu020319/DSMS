<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <el-row :gutter="10" align="middle">
        <el-col :span="6">
          <el-select v-model="selectedUserId" placeholder="选择用户(全部)" clearable @change="loadData">
            <el-option v-for="u in userList" :key="u.userId" :label="u.realName" :value="u.userId" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-radio-group v-model="statsType" @change="loadData">
            <el-radio-button value="yearly">年度统计</el-radio-button>
            <el-radio-button value="monthly">月度统计</el-radio-button>
            <el-radio-button value="daily">日度统计</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-col>
        <el-col :span="8" style="text-align: right">
          <el-button @click="handleExport">Excel导出</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <div ref="chartRef" style="height: 450px"></div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header><span style="font-weight: bold">花费明细</span></template>
      <el-table :data="statsData" border stripe>
        <el-table-column prop="year" label="年份" v-if="statsType === 'yearly'" />
        <el-table-column prop="month" label="月份" v-if="statsType === 'monthly'" />
        <el-table-column prop="day" label="日期" v-if="statsType === 'daily'" />
        <el-table-column label="花费金额(元)">
          <template #default="{ row }">¥{{ row.total_amount || row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="购药次数">
          <template #default="{ row }">{{ row.count || row.purchaseCount }}次</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getMonthlyStats, getDailyStats, getYearlyStats } from '../../api/purchase'
import { getUserList } from '../../api/user'
import { downloadFile } from '../../utils/download'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const chartRef = ref(null)
const statsData = ref([])
const selectedUserId = ref(null)
const statsType = ref('monthly')
const userList = ref([])
let chart = null

const loadData = async () => {
  let res
  if (statsType.value === 'yearly') {
    res = await getYearlyStats(selectedUserId.value)
  } else if (statsType.value === 'monthly') {
    res = await getMonthlyStats(selectedUserId.value)
  } else {
    res = await getDailyStats(selectedUserId.value, 90)
  }
  statsData.value = res.data
  await nextTick()
  renderChart()
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const labels = statsData.value.map(d => d.year || d.month || d.day)
  const amounts = statsData.value.map(d => d.total_amount || d.totalAmount)
  const counts = statsData.value.map(d => d.count || d.purchaseCount)

  const titleMap = { yearly: '年度花费统计', monthly: '月度花费统计', daily: '日度花费统计' }

  chart.setOption({
    title: { text: titleMap[statsType.value] },
    tooltip: { trigger: 'axis' },
    legend: { data: ['花费金额(元)', '购药次数'] },
    xAxis: { type: 'category', data: labels },
    yAxis: [
      { type: 'value', name: '金额(元)' },
      { type: 'value', name: '次数' }
    ],
    series: [
      { name: '花费金额(元)', type: 'bar', data: amounts, itemStyle: { color: '#409EFF' } },
      { name: '购药次数', type: 'line', yAxisIndex: 1, data: counts, itemStyle: { color: '#67C23A' } }
    ]
  })
}

const handleExport = async () => {
  try {
    await downloadFile('/export/purchase', { userId: selectedUserId.value }, '购药统计.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {}
}

onMounted(async () => {
  const uRes = await getUserList()
  userList.value = uRes.data
  loadData()
  window.addEventListener('resize', () => chart?.resize())
})
</script>
