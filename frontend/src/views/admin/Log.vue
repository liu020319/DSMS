<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <el-row :gutter="10" align="middle">
        <el-col :span="6">
          <el-select v-model="search.operationType" placeholder="操作类型" clearable @clear="loadData">
            <el-option-group label="用户管理">
              <el-option label="用户登录" value="用户登录" />
              <el-option label="新增用户" value="新增用户" />
              <el-option label="修改用户" value="修改用户" />
              <el-option label="重置密码" value="重置密码" />
              <el-option label="绑定老人" value="绑定老人" />
            </el-option-group>
            <el-option-group label="药品管理">
              <el-option label="新增药品" value="新增药品" />
              <el-option label="修改药品" value="修改药品" />
              <el-option label="禁用启用药品" value="禁用启用药品" />
            </el-option-group>
            <el-option-group label="用药方案">
              <el-option label="新增用药方案" value="新增用药方案" />
              <el-option label="修改用药方案" value="修改用药方案" />
              <el-option label="停用用药方案" value="停用用药方案" />
              <el-option label="启用用药方案" value="启用用药方案" />
            </el-option-group>
            <el-option-group label="购药记录">
              <el-option label="新增购药记录" value="新增购药记录" />
              <el-option label="修改购药记录" value="修改购药记录" />
              <el-option label="删除购药记录" value="删除购药记录" />
            </el-option-group>
            <el-option-group label="库存管理">
              <el-option label="分时段扣减" value="分时段扣减" />
              <el-option label="手动修正库存" value="手动修正库存" />
            </el-option-group>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-col>
        <el-col :span="4" style="text-align: right">
          <el-button @click="handleExport">导出日志</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="operationTime" label="操作时间" width="170" />
        <el-table-column prop="userId" label="用户" width="80">
          <template #default="{ row }">
            <span v-if="row.userId && row.userId > 0">{{ getUserName(row.userId) }}</span>
            <span v-else style="color:#999">系统</span>
          </template>
        </el-table-column>
        <el-table-column prop="operationType" label="操作类型" width="130">
          <template #default="{ row }">
            <el-tag :type="getTypeTagColor(row.operationType)" size="small">{{ row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationContent" label="操作内容" show-overflow-tooltip />
        <el-table-column prop="operationIp" label="IP" width="130" />
      </el-table>
      <el-pagination
        style="margin-top: 15px; justify-content: flex-end"
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogPage } from '../../api/log'
import { getUserList } from '../../api/user'
import { downloadFile } from '../../utils/download'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const userMap = ref({})
const page = reactive({ current: 1, size: 10, total: 0 })
const search = reactive({ operationType: '' })

const getUserName = (userId) => {
  return userMap.value[userId] || ('用户' + userId)
}

const getTypeTagColor = (type) => {
  if (!type) return 'info'
  if (type.includes('登录')) return 'success'
  if (type.includes('新增')) return 'primary'
  if (type.includes('修改') || type.includes('修正')) return 'warning'
  if (type.includes('删除') || type.includes('停用') || type.includes('禁用')) return 'danger'
  if (type.includes('扣减')) return ''
  return 'info'
}

const loadData = async () => {
  const res = await getLogPage({ current: page.current, size: page.size, ...search })
  tableData.value = res.data.records
  page.total = res.data.total
}

const handleExport = async () => {
  try {
    await downloadFile('/export/log', { operationType: search.operationType }, '操作日志.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {}
}

onMounted(async () => {
  try {
    const uRes = await getUserList()
    const map = {}
    uRes.data.forEach(u => { map[u.userId] = u.realName })
    userMap.value = map
  } catch (e) {}
  loadData()
})
</script>
