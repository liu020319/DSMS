<template>
  <el-image v-if="objectUrl" :src="objectUrl" :preview-src-list="preview ? [objectUrl] : []" :fit="fit">
    <template #error><div class="image-state">图片加载失败</div></template>
  </el-image>
  <div v-else class="image-state">{{ loading ? '图片加载中…' : '暂无图片' }}</div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import request from '../utils/request'

const props = defineProps({
  src: { type: String, default: '' },
  fit: { type: String, default: 'cover' },
  preview: { type: Boolean, default: true }
})
const objectUrl = ref('')
const loading = ref(false)

const release = () => {
  if (objectUrl.value) URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
}
const load = async value => {
  release()
  if (!value) return
  loading.value = true
  try {
    const path = value.startsWith('/api/') ? value.slice(4) : value
    const blob = await request.get(path, { responseType: 'blob' })
    objectUrl.value = URL.createObjectURL(blob)
  } finally {
    loading.value = false
  }
}
watch(() => props.src, load, { immediate: true })
onBeforeUnmount(release)
</script>

<style scoped>
.image-state{width:100%;height:100%;display:grid;place-items:center;color:#82918c;background:#edf2f0;font-size:12px}
</style>
