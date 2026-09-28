import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, describeError, request } from './client'

function mockFetch(response: Partial<Response> & { jsonBody?: unknown }) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => response.jsonBody,
    ...response,
  })
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => vi.unstubAllGlobals())

describe('request', () => {
  it('retorna o JSON em caso de sucesso e chama a URL base da API', async () => {
    const fetchMock = mockFetch({ jsonBody: [{ id: 1 }] })
    const data = await request<{ id: number }[]>('/eleitores')
    expect(data).toEqual([{ id: 1 }])
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/eleitores', expect.any(Object))
  })

  it('retorna undefined em respostas 204', async () => {
    mockFetch({ status: 204 })
    await expect(request<void>('/eleitores/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('converte respostas de erro em ApiError com mensagem e detalhes', async () => {
    mockFetch({ ok: false, status: 400, jsonBody: { message: 'Dados inválidos.', details: ['nome: não pode ser vazio'] } })
    const erro = (await request('/eleitores').catch((e: unknown) => e)) as ApiError
    expect(erro).toBeInstanceOf(ApiError)
    expect(erro.status).toBe(400)
    expect(describeError(erro)).toBe('Dados inválidos. nome: não pode ser vazio')
  })

  it('trata falha de conexão com mensagem amigável', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network')))
    const erro = (await request('/eleitores').catch((e: unknown) => e)) as ApiError
    expect(erro).toBeInstanceOf(ApiError)
    expect(erro.status).toBe(0)
    expect(erro.message).toMatch(/conectar à API/)
  })

  it('usa mensagem genérica quando o erro não traz JSON', async () => {
    mockFetch({ ok: false, status: 500, json: async () => { throw new Error('sem corpo') } })
    const erro = (await request('/eleitores').catch((e: unknown) => e)) as ApiError
    expect(erro.message).toBe('Erro 500')
  })
})
