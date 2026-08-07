import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
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
