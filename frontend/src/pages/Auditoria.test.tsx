import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import Auditoria from './Auditoria'

vi.mock('../api', () => ({
  api: { auditoria: { listar: vi.fn(), resumo: vi.fn() } },
}))
import { api } from '../api'

const mocked = vi.mocked(api, true)

const evento = {
  id: 1, eventId: 'abcdef12-0000-0000-0000-000000000000', eventType: 'VotoRegistrado',
  routingKey: 'voto.registrado', occurredAt: '2026-09-27T20:30:00Z', recebidoEm: '2026-09-27T20:30:01Z', payload: '{}',
}

beforeEach(() => {
  vi.clearAllMocks()
  mocked.auditoria.resumo.mockResolvedValue([
    { tipo: 'VotoRegistrado', total: 3 },
    { tipo: 'CandidatoCadastrado', total: 5 },
  ])
  mocked.auditoria.listar.mockResolvedValue([evento])
})

describe('Auditoria', () => {
  it('exibe o resumo por tipo e a lista de eventos', async () => {
    render(<Auditoria />)

    expect(await screen.findByText('abcdef12')).toBeInTheDocument()
    expect(screen.getByText('voto.registrado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /VotoRegistrado/ })).toHaveTextContent('3')
  })

  it('filtra a lista ao clicar em um tipo', async () => {
    const user = userEvent.setup()
    render(<Auditoria />)
    await screen.findByText('abcdef12')

    await user.click(screen.getByRole('button', { name: /CandidatoCadastrado/ }))

    await waitFor(() => expect(mocked.auditoria.listar).toHaveBeenLastCalledWith('CandidatoCadastrado'))
  })

  it('mostra erro quando o audit-service está indisponível', async () => {
    mocked.auditoria.resumo.mockRejectedValue(new ApiError(0, 'Não foi possível conectar à API.'))
    render(<Auditoria />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar')
  })

  it('informa quando não há eventos', async () => {
    mocked.auditoria.resumo.mockResolvedValue([])
    mocked.auditoria.listar.mockResolvedValue([])
    render(<Auditoria />)

    expect(await screen.findByText('Nenhum evento para exibir.')).toBeInTheDocument()
  })
})
