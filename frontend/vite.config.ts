import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 300000,       // 5분 — SSE 스트림 끊김 방지
        proxyTimeout: 300000,  // 5분
      },
    },
  },
})
