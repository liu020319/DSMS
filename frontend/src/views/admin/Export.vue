<template>
  <div>
    <el-card>
      <template #header><span style="font-weight: bold; font-size: 18px">📦 数据导出中心 - 全量本地备份</span></template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" style="text-align: center; cursor: pointer" @click="exportData('medicine', '药品档案数据.xlsx')">
            <el-icon style="font-size: 48px; color: #409EFF"><FirstAidKit /></el-icon>
            <div style="margin-top: 10px; font-weight: bold">药品档案数据</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" style="text-align: center; cursor: pointer" @click="exportData('user', '用户数据.xlsx')">
            <el-icon style="font-size: 48px; color: #67C23A"><User /></el-icon>
            <div style="margin-top: 10px; font-weight: bold">用户数据</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" style="text-align: center; cursor: pointer" @click="exportData('purchase', '购药记录数据.xlsx')">
            <el-icon style="font-size: 48px; color: #E6A23C"><ShoppingCart /></el-icon>
            <div style="margin-top: 10px; font-weight: bold">购药记录数据</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" style="text-align: center; cursor: pointer" @click="exportData('log', '操作日志数据.xlsx')">
            <el-icon style="font-size: 48px; color: #909399"><Notebook /></el-icon>
            <div style="margin-top: 10px; font-weight: bold">操作日志数据</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { downloadFile } from '../../utils/download'

const exportData = async (type, filename) => {
  try {
    await downloadFile('/export/' + type, {}, filename)
    ElMessage.success('导出成功')
  } catch (e) {}
}
</script>
