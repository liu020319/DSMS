import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 生产环境由 Nginx 挂载在 /kanglian-cloud/，让构建产物中的
  // JavaScript、CSS 等静态资源也从该子路径加载，避免误请求博客根目录 /assets/。
  base: '/kanglian-cloud/',
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-core': ['vue', 'vue-router', 'pinia', 'axios'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          'charts': ['echarts']
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      }
    }
  }
})
