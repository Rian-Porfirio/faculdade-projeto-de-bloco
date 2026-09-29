import { defineConfig, devices } from '@playwright/test'

// BASE_URL aponta para o frontend: http://localhost:8090 quando o sistema sobe via docker compose
// (ver docker-compose.yml), ou http://localhost:5173 rodando `npm run dev` localmente.
export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // os testes compartilham o mesmo backend/estado (dados de demonstração)
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['github']] : 'list',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:8090',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
