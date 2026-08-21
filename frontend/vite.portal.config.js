import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  // 统一门户部署在独立子目录，不能与博客根目录或 DSMS 的资源路径混用。
  base: '/cloud-hub/',
  root: fileURLToPath(new URL('./portal', import.meta.url)),
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./portal/src', import.meta.url)) }
  },
  build: {
    outDir: fileURLToPath(new URL('./portal-dist', import.meta.url)),
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-core': ['vue', 'vue-router', 'axios'],
          'element-plus': ['element-plus']
        }
      }
    }
  },
  server: {
    port: 5174,
    proxy: { '/api': { target: 'http://127.0.0.1:8088', changeOrigin: true } }
  }
})
