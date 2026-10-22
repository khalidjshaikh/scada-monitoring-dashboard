import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const bindHost = process.env.VITE_HOST ?? '::'

export default defineConfig({
  plugins: [react()],
  server: {
    host: bindHost,
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_TARGET ?? 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: bindHost,
    port: 4173,
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
})
