import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: { chunkSizeWarningLimit: 1100, rollupOptions: { output: { manualChunks: { vue: ['vue', 'vue-router', 'pinia'], element: ['element-plus', '@element-plus/icons-vue'], charts: ['echarts'], http: ['axios'] } } } },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
