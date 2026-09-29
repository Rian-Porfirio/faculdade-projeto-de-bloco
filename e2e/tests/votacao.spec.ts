import { expect, test } from '@playwright/test'

/**
 * Fluxo principal ponta a ponta (Usuário -> Frontend -> API -> voto -> RabbitMQ -> processamento -> resultado),
 * contra o sistema completo (todos os containers do docker-compose.yml).
 */
test.describe('Fluxo de votação', () => {
  test('vota, recebe comprovante e vê o resultado atualizado', async ({ page }) => {
    await page.goto('/votacao')

    await page.getByLabel('Eleitor').selectOption({ label: /Fernanda Lima/ })
    await page.getByRole('button', { name: /Ana Ribeiro/ }).click()
    await page.getByRole('button', { name: 'Confirmar voto' }).click()

    await expect(page.getByRole('heading', { name: 'Voto registrado' })).toBeVisible()
    const comprovante = await page.locator('dd.mono').last().innerText()
    expect(comprovante).toMatch(/^#\d+$/)

    // O result-service processa o evento de forma assíncrona: esperamos a projeção refletir o voto nos filtros.
    await page.goto('/resultado')
    await page.getByLabel('Fonte dos dados').selectOption('eventos')
    await expect(page.getByText(/voto\(s\) apurados/)).toContainText(/[1-9]\d* voto/, { timeout: 15000 })
  })

  test('impede um segundo voto do mesmo eleitor na mesma eleição', async ({ page }) => {
    await page.goto('/votacao')
    await page.getByLabel('Eleitor').selectOption({ label: /Gustavo Rocha/ })
    await page.getByRole('button', { name: /Ana Ribeiro/ }).click()
    await page.getByRole('button', { name: 'Confirmar voto' }).click()
    await expect(page.getByRole('heading', { name: 'Voto registrado' })).toBeVisible()

    await page.getByRole('button', { name: 'Registrar outro voto' }).click()
    await page.getByLabel('Eleitor').selectOption({ label: /Gustavo Rocha/ })
    await page.getByRole('button', { name: /Bruno Tavares/ }).click()
    await page.getByRole('button', { name: 'Confirmar voto' }).click()

    await expect(page.getByRole('alert')).toContainText('já votou')
  })
})

test.describe('Auditoria', () => {
  test('mostra eventos publicados pelo voting-service', async ({ page }) => {
    // Gera pelo menos um evento antes de conferir a auditoria.
    await page.goto('/votacao')
    await page.getByLabel('Eleitor').selectOption({ label: /Helena Souza/ })
    await page.getByRole('button', { name: /Carla Mendes/ }).click()
    await page.getByRole('button', { name: 'Confirmar voto' }).click()
    await expect(page.getByRole('heading', { name: 'Voto registrado' })).toBeVisible()

    await page.goto('/auditoria')
    await expect(page.getByRole('button', { name: /VotoRegistrado/ })).toBeVisible({ timeout: 15000 })
    await page.getByRole('button', { name: /VotoRegistrado/ }).click()
    await expect(page.getByText('voto.registrado').first()).toBeVisible()
  })
})

test.describe('CRUD', () => {
  test('cadastra um novo eleitor e ele aparece na lista', async ({ page }) => {
    await page.goto('/eleitores')
    const identificador = `E2E-${Date.now()}`

    await page.getByLabel('Nome').fill('Eleitor de Teste E2E')
    await page.getByLabel('Identificador').fill(identificador)
    await page.getByLabel('Estado').selectOption('SP')
    await page.getByLabel('Cidade').fill('Santos')
    await page.getByLabel('Local de votação').selectOption({ index: 1 })
    await page.getByRole('button', { name: 'Cadastrar eleitor' }).click()

    await expect(page.getByRole('status')).toContainText('Eleitor cadastrado.')
    await expect(page.getByText(identificador)).toBeVisible()
  })
})
