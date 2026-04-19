import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const apiPort = process.env.MINDFUL_FINANCE_API_PORT ?? '8080'
const frontendPort = Number.parseInt(
  process.env.MINDFUL_FINANCE_FRONTEND_PORT ?? '5173',
  10,
)
const proxyTarget =
  process.env.VITE_DEV_PROXY_TARGET ?? `http://localhost:${apiPort}`

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: Number.isNaN(frontendPort) ? 5173 : frontendPort,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
