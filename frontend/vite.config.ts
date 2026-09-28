/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em desenvolvimento, cada prefixo é encaminhado ao serviço correspondente (8080, 8081 e 8082).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }, // voting-service
      '/result-api': { target: 'http://localhost:8081', changeOrigin: true }, // result-service (Branch 2)
      '/audit-api': { target: 'http://localhost:8082', changeOrigin: true }, // audit-service (Branch 2)
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: false,
  },
})
