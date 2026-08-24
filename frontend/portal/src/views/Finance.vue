<template>
  <div class="feature-page finance-studio">
    <div class="finance-ambient" aria-hidden="true"><i></i><i></i><i></i></div>
    <div class="page-heading finance-heading">
      <div><p class="overline dark">FLOW LEDGER · PERSONAL SPACE</p><h1>把记账压缩到十秒内</h1><p>每位朋友只看得到自己的账本。常用分类和消费地点会跟着使用自动浮到前面。</p></div>
      <div class="heading-actions"><el-select v-model="ledgerId" placeholder="选择账本" @change="reload"><el-option v-for="l in ledgers" :key="l.ledgerId" :label="l.ledgerName" :value="l.ledgerId" /></el-select><el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="reload" /><el-button @click="ledgerDialog=true">新建账本</el-button></div>
    </div>

    <el-empty v-if="!loading && !ledgers.length" description="先创建一个只属于你的个人账本"><el-button type="primary" @click="ledgerDialog=true">创建账本</el-button></el-empty>
    <template v-else-if="ledgerId">
      <section class="quick-capture">
        <div class="capture-copy"><span>QUICK CAPTURE</span><h2>刚刚这笔，花了多少？</h2><p>选金额、点场景、保存。没有的选项直接输入一次，下次它会成为你的常用选项。</p></div>
        <div class="amount-stage"><small>{{ quickForm.transactionType==='EXPENSE'?'支出':'收入' }}</small><b>¥</b><el-input-number v-model="quickForm.amount" :min="0.01" :precision="2" :controls="false" /><el-segmented v-model="quickForm.transactionType" :options="[{label:'支出',value:'EXPENSE'},{label:'收入',value:'INCOME'}]" @change="syncQuickOptions" /></div>
        <div class="capture-controls">
          <label>消费分类</label>
          <div class="quick-chips"><button v-for="item in quickCategories" :key="item.name" :class="{active:quickForm.categoryName===item.name}" @click="quickForm.categoryName=item.name"><span>{{item.icon}}</span>{{item.name}}</button><button class="custom-chip" @click="focusCustom('category')">＋ 自定义</button></div>
          <label>花在哪里 / 交易对象</label>
          <div class="quick-chips merchant-chips"><button v-for="name in quickCounterparties" :key="name" :class="{active:quickForm.counterparty===name}" @click="quickForm.counterparty=name">{{name}}</button><button class="custom-chip" @click="focusCustom('counterparty')">＋ 新地点</button></div>
          <div class="quick-custom-row"><el-select ref="categorySelect" v-model="quickForm.categoryName" filterable allow-create default-first-option placeholder="选择或输入分类"><el-option v-for="item in allCategoryOptions" :key="item.name" :label="item.name" :value="item.name" /></el-select><el-select ref="counterpartySelect" v-model="quickForm.counterparty" filterable allow-create default-first-option clearable placeholder="选择或输入消费地点"><el-option v-for="name in allCounterpartyOptions" :key="name" :label="name" :value="name" /></el-select><el-select v-if="accounts.length" v-model="quickForm.accountId" placeholder="付款账户"><el-option v-for="a in accounts" :key="a.accountId" :label="a.accountName" :value="a.accountId" /></el-select><button v-else class="missing-account-button" @click="openAccountDialog">＋ 创建付款账户</button><button class="capture-button" :disabled="savingQuick||!accounts.length" @click="quickSave"><span>{{savingQuick?'保存中':'记一笔'}}</span><small>ENTER TO SAVE</small></button></div>
        </div>
      </section>

      <section class="metric-grid finance-metrics"><article><small>本月支出</small><b>¥ {{ money(summary.expense) }}</b><i>EXPENSE</i><em>共 {{ expenseCount }} 笔</em></article><article><small>日均支出</small><b>¥ {{ money(dailyAverage) }}</b><i>DAILY AVG</i><em>{{ activeDays }} 个消费日</em></article><article><small>最常去向</small><b class="text-metric">{{ topCounterparty.name }}</b><i>TOP PLACE</i><em>¥ {{money(topCounterparty.amount)}}</em></article><article><small>预算剩余</small><b :class="{ danger: Number(summary.budgetRemaining) < 0 }">¥ {{ money(summary.budgetRemaining) }}</b><i>BUDGET</i><em>{{ budgetSignal }}</em></article></section>

      <section class="finance-insight-grid">
        <article class="panel spending-radar"><div class="panel-head"><div><small>SPENDING MAP</small><h3>钱都流向了哪里</h3></div><el-segmented v-model="insightMode" :options="[{label:'按分类',value:'category'},{label:'按地点',value:'counterparty'}]" /></div><div v-if="insightRows.length" class="category-bars rich-bars"><div v-for="(item,index) in insightRows" :key="item.name"><span><i>{{String(index+1).padStart(2,'0')}}</i>{{item.name}}</span><em><b :style="{width:item.percent+'%'}"></b></em><strong>¥{{money(item.amount)}}</strong></div></div><el-empty v-else description="记下第一笔后，这里会生成消费地图" :image-size="70" /></article>
        <article class="panel pulse-card"><div class="panel-head"><div><small>MONTHLY PULSE</small><h3>本月消费脉冲</h3></div><b>{{month}}</b></div><div v-if="dayRows.length" class="day-pulse"><i v-for="day in dayRows" :key="day.day" :style="{height:day.percent+'%'}" :title="`${day.day} ¥${money(day.amount)}`"></i></div><el-empty v-else description="暂无本月消费" :image-size="58" /><footer><span>月初</span><span>今天 / 月末</span></footer><div class="largest-expense" v-if="summary.largestExpense"><span>本月单笔最高</span><b>¥{{money(summary.largestExpense.amount)}}</b><small>{{summary.largestExpense.counterparty||summary.largestExpense.categoryName}}</small></div></article>
      </section>

      <section class="two-column finance-lower">
        <article class="panel"><div class="panel-head"><div><small>ACCOUNTS</small><h3>资金账户</h3></div><el-button text @click="openAccountDialog">新增账户</el-button></div><el-empty v-if="!accounts.length" description="先创建付款账户，创建后即可记第一笔" :image-size="64"><el-button type="primary" @click="openAccountDialog">创建付款账户</el-button></el-empty><div v-else class="account-list"><div v-for="a in accounts" :key="a.accountId"><span>{{ accountIcon[a.accountType] || '□' }}</span><div><b>{{a.accountName}}</b><small>{{ accountName[a.accountType] || a.accountType }}</small></div><strong>初始 ¥{{money(a.initialBalance)}}</strong><el-button text type="danger" @click="removeAccount(a)">删除</el-button></div></div></article>
        <article class="panel budget-card"><div class="panel-head"><div><small>BUDGET GUARD</small><h3>预算防线</h3></div><el-button text @click="budgetDialog=true">设置预算</el-button></div><div class="budget-ring" :style="{'--progress':Math.min(100,budgetPercent)+'%'}"><div><b>{{Math.round(budgetPercent)}}%</b><small>预算使用</small></div></div><p>{{ budgetSignal }}。预算参考主流产品的临界提醒思路，本系统在页面内以颜色提示，不额外产生短信费用。</p></article>
      </section>

      <section class="panel transactions"><div class="panel-head"><div><small>TRANSACTION STREAM</small><h3>收支时间线</h3></div><el-button type="primary" @click="openAdvanced">详细登记</el-button></div>
        <div class="desktop-table"><el-table :data="transactions"><el-table-column prop="transactionTime" label="时间" min-width="165" /><el-table-column prop="categoryName" label="分类" min-width="100" /><el-table-column prop="counterparty" label="花在哪里" min-width="135" /><el-table-column prop="note" label="备注" min-width="160" /><el-table-column label="金额" width="130"><template #default="s"><b :class="s.row.transactionType==='INCOME'?'positive':'danger'">{{s.row.transactionType==='INCOME'?'+':'-'}}¥{{money(s.row.amount)}}</b></template></el-table-column><el-table-column label="操作" width="80"><template #default="s"><el-button text type="danger" @click="remove(s.row.transactionId)">删除</el-button></template></el-table-column></el-table></div>
        <div class="mobile-list"><article v-for="t in transactions" :key="t.transactionId"><div><b>{{t.categoryName}} · {{t.counterparty||'未填写去向'}}</b><small>{{t.transactionTime}}</small></div><strong :class="t.transactionType==='INCOME'?'positive':'danger'">{{t.transactionType==='INCOME'?'+':'-'}}¥{{money(t.amount)}}</strong><button @click="remove(t.transactionId)">删除</button></article></div>
      </section>
    </template>

    <el-dialog v-model="ledgerDialog" title="新建个人账本" width="min(440px,92vw)"><el-input v-model="ledgerForm.ledgerName" maxlength="80" placeholder="例如：我的日常账本" /><template #footer><el-button @click="ledgerDialog=false">取消</el-button><el-button type="primary" @click="createLedger">创建</el-button></template></el-dialog>
    <el-dialog v-model="accountDialog" title="新增资金账户" width="min(500px,92vw)"><el-form label-position="top"><el-form-item label="账户名称"><el-input v-model="accountForm.accountName" /></el-form-item><el-form-item label="账户类型"><el-select v-model="accountForm.accountType" class="full"><el-option v-for="(label,key) in accountName" :key="key" :label="label" :value="key" /></el-select></el-form-item><el-form-item label="初始余额"><el-input-number v-model="accountForm.initialBalance" :precision="2" :step="100" class="full" /></el-form-item></el-form><template #footer><el-button @click="accountDialog=false">取消</el-button><el-button type="primary" @click="createAccount">保存</el-button></template></el-dialog>
    <el-dialog v-model="transactionDialog" title="详细登记收支" width="min(600px,94vw)"><el-form label-position="top"><div class="form-grid"><el-form-item label="类型"><el-segmented v-model="transactionForm.transactionType" :options="[{label:'支出',value:'EXPENSE'},{label:'收入',value:'INCOME'}]" /></el-form-item><el-form-item label="账户"><el-select v-model="transactionForm.accountId" class="full"><el-option v-for="a in accounts" :key="a.accountId" :label="a.accountName" :value="a.accountId" /></el-select></el-form-item><el-form-item label="分类"><el-select v-model="transactionForm.categoryName" filterable allow-create default-first-option class="full"><el-option v-for="item in allCategoryOptions" :key="item.name" :label="item.name" :value="item.name" /></el-select></el-form-item><el-form-item label="金额"><el-input-number v-model="transactionForm.amount" :min="0.01" :precision="2" class="full" /></el-form-item><el-form-item label="时间"><el-date-picker v-model="transactionForm.transactionTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" class="full" /></el-form-item><el-form-item label="花在哪里 / 交易对象"><el-select v-model="transactionForm.counterparty" filterable allow-create clearable class="full"><el-option v-for="name in allCounterpartyOptions" :key="name" :label="name" :value="name" /></el-select></el-form-item></div><el-form-item label="备注"><el-input v-model="transactionForm.note" type="textarea" maxlength="500" /></el-form-item></el-form><template #footer><el-button @click="transactionDialog=false">取消</el-button><el-button type="primary" @click="createTransaction">保存流水</el-button></template></el-dialog>
    <el-dialog v-model="budgetDialog" title="设置分类预算" width="min(460px,92vw)"><el-form label-position="top"><el-form-item label="分类"><el-select v-model="budgetForm.categoryName" filterable allow-create class="full"><el-option v-for="item in allCategoryOptions" :key="item.name" :label="item.name" :value="item.name" /></el-select></el-form-item><el-form-item label="本月预算"><el-input-number v-model="budgetForm.budgetAmount" :min="0.01" :precision="2" class="full" /></el-form-item></el-form><template #footer><el-button @click="budgetDialog=false">取消</el-button><el-button type="primary" @click="saveBudget">保存</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { financeApi, reportError } from '../api'

const defaultExpense=[['餐饮','◌'],['零食','◇'],['奶茶','◉'],['水果','✦'],['交通','↗'],['购物','▣'],['网购','⌁'],['生活用品','□'],['游戏充值','△'],['娱乐','◎'],['房租水电','⌂'],['医疗','＋']].map(([name,icon])=>({name,icon}))
const defaultIncome=[['工资','¥'],['兼职','↗'],['红包','◇'],['退款','↩'],['其他收入','＋']].map(([name,icon])=>({name,icon}))
const merchantSeeds=['零食顽家','馋不二','王者荣耀','和平精英','滴滴出行','淘宝','京东','美团','饿了么','蜜雪冰城','火锅','烧烤']
const nowMonth=new Date().toISOString().slice(0,7)
const nowLocal=()=>{const d=new Date(),p=n=>String(n).padStart(2,'0');return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`}
const loading=ref(false),savingQuick=ref(false),ledgers=ref([]),ledgerId=ref(),month=ref(nowMonth),accounts=ref([]),transactions=ref([]),summary=ref({}),suggestions=ref({}),insightMode=ref('category')
const ledgerDialog=ref(false),accountDialog=ref(false),transactionDialog=ref(false),budgetDialog=ref(false),categorySelect=ref(),counterpartySelect=ref()
const ledgerForm=reactive({ledgerName:''}),accountForm=reactive({accountName:'日常账户',accountType:'WECHAT',initialBalance:0})
const quickForm=reactive({transactionType:'EXPENSE',accountId:null,categoryName:'餐饮',amount:0.01,counterparty:''})
const transactionForm=reactive({transactionType:'EXPENSE',accountId:null,categoryName:'',amount:0.01,transactionTime:nowLocal(),counterparty:'',note:''})
const budgetForm=reactive({categoryName:'餐饮',budgetAmount:1000})
const accountName={CASH:'现金',BANK:'银行卡',WECHAT:'微信',ALIPAY:'支付宝',OTHER:'其他'},accountIcon={CASH:'¥',BANK:'▣',WECHAT:'◉',ALIPAY:'A',OTHER:'□'}
const money=v=>Number(v||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})
const learnedCategories=computed(()=>((quickForm.transactionType==='EXPENSE'?suggestions.value.expenseCategories:suggestions.value.incomeCategories)||[]).map(x=>({name:x.name,icon:'↗'})))
const quickCategories=computed(()=>{const all=[...learnedCategories.value,...(quickForm.transactionType==='EXPENSE'?defaultExpense:defaultIncome)];return [...new Map(all.map(x=>[x.name,x])).values()].slice(0,10)})
const allCategoryOptions=computed(()=>[...new Map([...learnedCategories.value,...defaultExpense,...defaultIncome].map(x=>[x.name,x])).values()])
const learnedCounterparties=computed(()=>(suggestions.value.counterparties||[]).map(x=>x.name))
const allCounterpartyOptions=computed(()=>[...new Set([...learnedCounterparties.value,...merchantSeeds])])
const quickCounterparties=computed(()=>allCounterpartyOptions.value.slice(0,9))
const mapRows=(data={})=>{const entries=Object.entries(data).sort((a,b)=>Number(b[1])-Number(a[1]));const max=Math.max(...entries.map(x=>Number(x[1])),1);return entries.map(([name,amount])=>({name,amount,percent:Math.max(5,Number(amount)/max*100)}))}
const insightRows=computed(()=>mapRows(insightMode.value==='category'?summary.value.expenseByCategory:summary.value.expenseByCounterparty))
const dayRows=computed(()=>mapRows(summary.value.expenseByDay).sort((a,b)=>a.name.localeCompare(b.name)).map(x=>({day:x.name,amount:x.amount,percent:Math.max(8,x.percent)})))
const activeDays=computed(()=>Object.keys(summary.value.expenseByDay||{}).length)
const expenseCount=computed(()=>Number(summary.value.expenseTransactionCount||0))
const dailyAverage=computed(()=>activeDays.value?Number(summary.value.expense||0)/activeDays.value:0)
const topCounterparty=computed(()=>(mapRows(summary.value.expenseByCounterparty)[0]||{name:'暂无',amount:0}))
const budgetPercent=computed(()=>Number(summary.value.budget)>0?Number(summary.value.expense||0)/Number(summary.value.budget)*100:0)
const budgetSignal=computed(()=>!Number(summary.value.budget)?'尚未设置本月预算':budgetPercent.value>=100?'预算已超出，请留意后续消费':budgetPercent.value>=90?'已进入 90% 预警区':budgetPercent.value>=75?'已进入 75% 提醒区':'预算状态健康')
const loadLedgers=async()=>{loading.value=true;try{ledgers.value=(await financeApi.ledgers()).data||[];if(!ledgerId.value&&ledgers.value.length)ledgerId.value=ledgers.value[0].ledgerId;if(ledgerId.value)await reload()}catch(e){reportError(e)}finally{loading.value=false}}
const reload=async()=>{if(!ledgerId.value)return;try{const [a,t,s,g]=await Promise.all([financeApi.accounts(ledgerId.value),financeApi.transactions(ledgerId.value,month.value),financeApi.summary(ledgerId.value,month.value),financeApi.suggestions(ledgerId.value)]);accounts.value=a.data||[];transactions.value=t.data||[];summary.value=s.data||{};suggestions.value=g.data||{};const accountIds=new Set(accounts.value.map(item=>item.accountId));quickForm.accountId=accountIds.has(quickForm.accountId)?quickForm.accountId:(accounts.value[0]?.accountId||null);transactionForm.accountId=accountIds.has(transactionForm.accountId)?transactionForm.accountId:(accounts.value[0]?.accountId||null)}catch(e){reportError(e)}}
const syncQuickOptions=()=>{quickForm.categoryName=(quickForm.transactionType==='EXPENSE'?defaultExpense:defaultIncome)[0].name;if(quickForm.transactionType==='INCOME')quickForm.counterparty=''}
const focusCustom=async type=>{await nextTick();const target=type==='category'?categorySelect.value:counterpartySelect.value;target?.focus?.()}
const quickSave=async()=>{if(!quickForm.accountId)return ElMessage.warning('请先新增并选择一个付款账户');if(!quickForm.categoryName.trim())return ElMessage.warning('请选择或输入分类');savingQuick.value=true;try{await financeApi.createTransaction({...quickForm,ledgerId:ledgerId.value,transactionTime:nowLocal(),note:null});quickForm.amount=.01;await reload();ElMessage.success(`已记下：${quickForm.categoryName}${quickForm.counterparty?' · '+quickForm.counterparty:''}`)}catch(e){reportError(e)}finally{savingQuick.value=false}}
const createLedger=async()=>{if(!ledgerForm.ledgerName.trim())return ElMessage.warning('请输入账本名称');try{const r=await financeApi.createLedger(ledgerForm);ledgerDialog.value=false;ledgerForm.ledgerName='';ledgerId.value=r.data.ledgerId;await loadLedgers();ElMessage.success('账本和默认付款账户已创建')}catch(e){reportError(e)}}
const openAccountDialog=()=>{accountForm.accountName=accounts.value.length?'':'日常账户';accountDialog.value=true}
const createAccount=async()=>{if(!accountForm.accountName.trim())return ElMessage.warning('请输入账户名称');try{await financeApi.createAccount({...accountForm,ledgerId:ledgerId.value});accountDialog.value=false;accountForm.accountName='';await reload();ElMessage.success('账户已创建')}catch(e){reportError(e)}}
const removeAccount=async account=>{try{await ElMessageBox.confirm(`确定删除“${account.accountName}”吗？已有流水的账户会被系统保护，不能删除。`,'删除资金账户',{type:'warning'});await financeApi.deleteAccount(account.accountId);await reload();ElMessage.success('账户已删除')}catch(e){if(e!=='cancel'&&e!=='close')reportError(e)}}
const openAdvanced=()=>{transactionForm.transactionTime=nowLocal();transactionDialog.value=true}
const createTransaction=async()=>{if(!transactionForm.accountId||!transactionForm.categoryName.trim())return ElMessage.warning('请补全账户和分类');try{await financeApi.createTransaction({...transactionForm,ledgerId:ledgerId.value});transactionDialog.value=false;transactionForm.categoryName='';transactionForm.amount=.01;transactionForm.note='';await reload();ElMessage.success('流水已保存')}catch(e){reportError(e)}}
const saveBudget=async()=>{if(!budgetForm.categoryName.trim())return ElMessage.warning('请输入预算分类');try{await financeApi.saveBudget({...budgetForm,ledgerId:ledgerId.value,budgetMonth:month.value});budgetDialog.value=false;await reload();ElMessage.success('预算已保存')}catch(e){reportError(e)}}
const remove=async id=>{try{await ElMessageBox.confirm('删除后无法恢复，确定删除这笔流水？','确认删除',{type:'warning'});await financeApi.deleteTransaction(id);await reload();ElMessage.success('已删除')}catch(e){if(e!=='cancel'&&e!=='close')reportError(e)}}
onMounted(loadLedgers)
</script>
