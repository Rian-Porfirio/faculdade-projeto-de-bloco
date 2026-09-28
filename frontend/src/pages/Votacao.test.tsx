import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { candidatoAna, candidatoBruno, eleicaoAtiva, eleitorMaria } from '../test/fixtures'
import Votacao from './Votacao'

vi.mock('../api', () => ({
  api: {
    eleicoes: { listar: vi.fn() },
    eleitores: { listar: vi.fn() },
    candidatos: { listar: vi.fn() },
    votos: { registrar: vi.fn() },
  },
}))
import { api } from '../api'

const mocked = vi.mocked(api, true)

beforeEach(() => {
  vi.clearAllMocks()
  mocked.eleicoes.listar.mockResolvedValue([eleicaoAtiva, { ...eleicaoAtiva, id: 2, status: 'ENCERRADA' }])
  mocked.eleitores.listar.mockResolvedValue([eleitorMaria])
  mocked.candidatos.listar.mockResolvedValue([candidatoAna, candidatoBruno])
})

async function preencherVoto(user: ReturnType<typeof userEvent.setup>) {
  await screen.findByRole('heading', { name: /Identifique o eleitor/ })
  await user.selectOptions(screen.getByLabelText('Eleitor'), '1')
  await user.click(await screen.findByRole('button', { name: /Ana Ribeiro/ }))
}

describe('Votacao', () => {
  it('lista apenas eleições ativas e os candidatos da eleição', async () => {
    render(<Votacao />)
    await screen.findByRole('button', { name: /Ana Ribeiro/ })
    expect(screen.getByRole('option', { name: 'Eleição Simulada 2026' })).toBeInTheDocument()
    expect(screen.getAllByRole('option', { name: /Eleição Simulada/ })).toHaveLength(1)
    expect(mocked.candidatos.listar).toHaveBeenCalledWith(1)
  })

  it('mantém o botão de confirmar desabilitado até escolher eleitor e candidato', async () => {
    render(<Votacao />)
    await screen.findByRole('button', { name: /Ana Ribeiro/ })
    expect(screen.getByRole('button', { name: 'Confirmar voto' })).toBeDisabled()
  })

  it('envia o voto para a API e exibe o comprovante', async () => {
    const user = userEvent.setup()
    mocked.votos.registrar.mockResolvedValue({
      id: 99, eleitorId: 1, eleitorNome: 'Maria Silva', candidatoId: 10, candidatoNome: 'Ana Ribeiro',
      candidatoNumero: 10, eleicaoId: 1, localVotacaoId: 1, dataHora: '2026-09-27T20:30:00Z',
    })
    render(<Votacao />)

    await preencherVoto(user)
    await user.click(screen.getByRole('button', { name: 'Confirmar voto' }))

    expect(mocked.votos.registrar).toHaveBeenCalledWith({ eleitorId: 1, candidatoId: 10, eleicaoId: 1 })
    expect(await screen.findByRole('heading', { name: 'Voto registrado' })).toBeInTheDocument()
    expect(screen.getByText('#99')).toBeInTheDocument()
  })

  it('mostra a mensagem da API quando o eleitor já votou', async () => {
    const user = userEvent.setup()
    mocked.votos.registrar.mockRejectedValue(new ApiError(409, 'O eleitor já votou nesta eleição.'))
    render(<Votacao />)

    await preencherVoto(user)
    await user.click(screen.getByRole('button', { name: 'Confirmar voto' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('O eleitor já votou nesta eleição.')
    expect(screen.queryByRole('heading', { name: 'Voto registrado' })).not.toBeInTheDocument()
  })

  it('permite corrigir a escolha do candidato', async () => {
    const user = userEvent.setup()
    render(<Votacao />)
    await preencherVoto(user)

    await user.click(screen.getByRole('button', { name: 'Corrigir escolha' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar voto' })).toBeDisabled())
  })

  it('informa quando não há eleição ativa', async () => {
    mocked.eleicoes.listar.mockResolvedValue([{ ...eleicaoAtiva, status: 'ENCERRADA' }])
    render(<Votacao />)
    expect(await screen.findByText('Não há eleição ativa no momento.')).toBeInTheDocument()
  })
})
