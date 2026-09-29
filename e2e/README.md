# Testes E2E (Playwright)

Testam o sistema **completo** de fora para dentro (navegador real), contra os containers do
`docker-compose.yml` (frontend + 3 backends + PostgreSQL + RabbitMQ).

```bash
# na raiz do repositório
docker compose up --build -d

cd e2e
npm install
npx playwright install --with-deps chromium
npm test
```

`BASE_URL` (padrão `http://localhost:8090`) pode ser trocada para rodar contra `npm run dev` do frontend.
No CI (`.github/workflows/ci.yml`), este pacote roda como o job `e2e`, depois dos testes unitários e do
build do frontend.
