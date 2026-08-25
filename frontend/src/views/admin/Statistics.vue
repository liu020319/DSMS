<template>
  <div class="page">
    <div class="head"><div><span>家庭对账凭证</span><h1>购药费用分析</h1><p>选择任意日期范围，并按年、月、周、日、平台、渠道或时段汇总。</p></div><el-button type="primary" @click="exportExcel">导出完整Excel明细</el-button></div>
    <el-card shadow="never" class="filters">
      <div class="filter-grid">
        <el-select v-model="filter.userId" clearable placeholder="全部老人" @change="loadAll"><el-option v-for="u in users" :key="u.userId" :label="u.realName+'（'+u.username+'）'" :value="u.userId"/></el-select>
        <el-date-picker v-model="filter.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" :disabled-date="disableStart" @change="onDateChange"/>
        <el-date-picker v-model="filter.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" :disabled-date="disableEnd" @change="onDateChange"/>
        <el-select v-model="filter.year" clearable placeholder="选择年份" @change="onPeriodChange"><el-option v-for="year in years" :key="year" :label="year+'年'" :value="year"/></el-select>
        <el-select v-model="filter.month" clearable placeholder="选择月份" :disabled="!filter.year" @change="onPeriodChange"><el-option v-for="month in 12" :key="month" :label="month+'月'" :value="month"/></el-select>
        <el-select v-model="filter.platform" clearable placeholder="全部购药平台" @change="loadAll"><el-option v-for="item in platforms" :key="item" :label="item" :value="item"/></el-select>
        <el-select v-model="filter.channel" clearable placeholder="线上/线下" @change="loadAll"><el-option label="线上" value="ONLINE"/><el-option label="线下" value="OFFLINE"/></el-select>
        <el-select v-model="type" placeholder="统计维度" @change="loadChart"><el-option v-for="item in dimensions" :key="item.value" :label="item.label" :value="item.value"/></el-select>
        <el-button plain @click="resetFilters">重置筛选</el-button>
      </div>
      <p class="filter-tip">日期范围与年/月是两种快捷筛选方式：选择其中一种时会自动清空另一种，避免条件互相冲突。</p>
    </el-card>
    <section class="metrics"><article><small>累计购药支出</small><strong>¥{{money(summary.total_amount)}}</strong></article><article><small>购药订单/次数</small><strong>{{summary.order_count||0}}次</strong></article><article><small>线上购药金额</small><strong>¥{{money(summary.online_amount)}}</strong></article><article><small>线下购药金额</small><strong>¥{{money(summary.offline_amount)}}</strong></article></section>
    <el-card shadow="never" class="chart-card"><div ref="chartRef" class="chart"></div></el-card>
    <el-card shadow="never"><template #header><b>{{currentLabel}}明细</b></template><el-table :data="data" border stripe><el-table-column :prop="keyField" :label="currentLabel"/><el-table-column label="购药金额"><template #default="{row}"><b class="amount">¥{{money(row.total_amount)}}</b></template></el-table-column><el-table-column label="购药次数"><template #default="{row}">{{row.count||0}}次</template></el-table-column><el-table-column label="次均花费"><template #default="{row}">¥{{money(row.average_amount)}}</template></el-table-column></el-table></el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getUserList } from '../../api/user'
import { getYearlyStats, getMonthlyStats, getWeeklyStats, getDailyStats, getPlatformStats, getChannelStats, getTimeBucketStats, getExpenseSummary } from '../../api/purchase'
import { downloadFile } from '../../utils/download'
import { ElMessage } from 'element-plus'

const users=ref([]),type=ref('monthly'),data=ref([]),summary=ref({}),chartRef=ref(null)
const filter=reactive({userId:null,startDate:'',endDate:'',year:null,month:null,platform:'',channel:''})
const currentYear=new Date().getFullYear(),years=Array.from({length:12},(_,index)=>currentYear+1-index)
const platforms=['京东','淘宝','美团','饿了么','医院','线下药店']
const dimensions=[{label:'按年度汇总',value:'yearly'},{label:'按月度汇总',value:'monthly'},{label:'按周汇总',value:'weekly'},{label:'按日汇总',value:'daily'},{label:'按平台汇总',value:'platform'},{label:'按线上/线下汇总',value:'channel'},{label:'按购药时段汇总',value:'time'}]
const meta={yearly:['year','年份'],monthly:['month','月份'],weekly:['week','周'],daily:['day','日期'],platform:['name','购药平台'],channel:['name','购买渠道'],time:['name','购药时段']}
const loaders={yearly:getYearlyStats,monthly:getMonthlyStats,weekly:getWeeklyStats,daily:getDailyStats,platform:getPlatformStats,channel:getChannelStats,time:getTimeBucketStats}
const keyField=computed(()=>meta[type.value][0]),currentLabel=computed(()=>meta[type.value][1])
const money=value=>Number(value||0).toFixed(2)
let chart
const params=()=>Object.fromEntries(Object.entries(filter).filter(([,value])=>value!==null&&value!==''))
const loadChart=async()=>{const res=await loaders[type.value](params());data.value=res.data||[];await nextTick();render()}
const loadAll=async()=>{const res=await getExpenseSummary(params());summary.value=res.data||{};await loadChart()}
const onDateChange=()=>{if(filter.startDate||filter.endDate){filter.year=null;filter.month=null}loadAll()}
const onPeriodChange=()=>{if(!filter.year)filter.month=null;if(filter.year||filter.month){filter.startDate='';filter.endDate=''}loadAll()}
const resetFilters=()=>{Object.assign(filter,{userId:null,startDate:'',endDate:'',year:null,month:null,platform:'',channel:''});loadAll()}
const disableStart=date=>filter.endDate&&date.getTime()>new Date(filter.endDate+'T23:59:59').getTime()
const disableEnd=date=>filter.startDate&&date.getTime()<new Date(filter.startDate+'T00:00:00').getTime()
const render=()=>{if(!chart)chart=echarts.init(chartRef.value);const labels=data.value.map(item=>item[keyField.value]);chart.setOption({tooltip:{trigger:'axis'},legend:{data:['购药金额','购药次数']},grid:{left:55,right:55,bottom:55},xAxis:{type:'category',data:labels,axisLabel:{rotate:labels.length>8?35:0}},yAxis:[{type:'value',name:'金额(元)'},{type:'value',name:'次数'}],series:[{name:'购药金额',type:'bar',data:data.value.map(item=>Number(item.total_amount||0)),itemStyle:{color:'#397c64',borderRadius:[6,6,0,0]}},{name:'购药次数',type:'line',yAxisIndex:1,smooth:true,data:data.value.map(item=>Number(item.count||0)),itemStyle:{color:'#d88d35'}}]},true)}
const resizeChart=()=>chart?.resize()
const exportExcel=async()=>{await downloadFile('/export/purchase',{userId:filter.userId},'购药费用凭证明细.xlsx');ElMessage.success('Excel已导出')}
onMounted(async()=>{const res=await getUserList('ELDER');users.value=res.data||[];await loadAll();window.addEventListener('resize',resizeChart)})
onBeforeUnmount(()=>window.removeEventListener('resize',resizeChart))
</script>

<style scoped>
.page{max-width:1200px;margin:auto}.head{display:flex;justify-content:space-between;align-items:end;margin-bottom:16px}.head span{color:#3a8066;font-weight:700}.head h1{margin:5px 0}.head p{margin:0;color:#75817a}.filters{margin-bottom:14px}.filter-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.filter-grid :deep(.el-date-editor),.filter-grid :deep(.el-select){width:100%}.filter-tip{margin:11px 0 0;color:#7a8780;font-size:13px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:14px}.metrics article{padding:18px;border-radius:15px;background:#fff;border:1px solid #e4ebe6}.metrics small,.metrics strong{display:block}.metrics small{color:#77827a}.metrics strong{font-size:25px;color:#34734e;margin-top:8px}.chart-card{margin-bottom:14px}.chart{height:430px}.amount{color:#34734e}@media(max-width:800px){.head{align-items:stretch;flex-direction:column;gap:12px}.filter-grid{grid-template-columns:1fr 1fr}.metrics{grid-template-columns:1fr 1fr}.chart{height:350px}}@media(max-width:430px){.filter-grid,.metrics{grid-template-columns:1fr}.metrics strong{font-size:22px}}
</style>
