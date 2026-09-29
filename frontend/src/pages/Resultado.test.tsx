import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { eleicaoAtiva, resultado } from '../test/fixtures'
import Resultado from './Resultado'

vi.mock('../api', () => ({
  api: { eleicoes: { listar: vi.fn() }, resultados: { apurar: vi.fn() } },
}))
import { api } from '../api'

const mocked = vi.mocked(api, true)

beforeEach(() => {
  vi.clearAllMocks()
  mocked.eleicoes.listar.mockResolvedValue([eleicaoAtiva])
  mocked.resultados.apurar.mockResolvedValue(resultado)
})

describe('Resultado', () => {
  it('exibe candidatos, votos, percentual e total', async () => {
    render(<Resultado />)

    expect(await screen.findByText(/voto\(s\) apurados/)).toHaveTextContent('4 voto(s) apurados')
    expect(screen.getByText(/Ana Ribeiro/)).toBeInTheDocument()
    expect(screen.getByText('75,0%')).toBeInTheDocument()
    expect(screen.getByText('25,0%')).toBeInTheDocument()
  })

  it('reconsulta a API ao filtrar por cargo e por estado', async () => {
    const user = userEvent.setup()
    render(<Resultado />)
    await screen.findByText(/voto\(s\) apurados/)

    await user.selectOptions(screen.getByLabelText('Cargo'), 'PRESIDENTE')
    await user.selectOptions(screen.getByLabelText('Estado do eleitor'), 'SP')

    await waitFor(() =>
      expect(mocked.resultados.apurar).toHaveBeenLastCalledWith(
        { eleicaoId: undefined, cargo: 'PRESIDENTE', estado: 'SP' },
        'eventos',
      ),
    )
  })

  it('consulta a projeção por eventos por padrão e permite trocar para o voting-service', async () => {
    const user = userEvent.setup()
    render(<Resultado />)
    await screen.findByText(/voto\(s\) apurados/)
    expect(mocked.resultados.apurar).toHaveBeenLastCalledWith({ eleicaoId: undefined, cargo: '', estado: '' }, 'eventos')
    expect(screen.getByText(/consistência eventual/)).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Fonte dos dados'), 'voting')

    await waitFor(() =>
      expect(mocked.resultados.apurar).toHaveBeenLastCalledWith({ eleicaoId: undefined, cargo: '', estado: '' }, 'voting'),
    )
    expect(screen.queryByText(/consistência eventual/)).not.toBeInTheDocument()
  })

  it('exibe erro da API', async () => {
    mocked.resultados.apurar.mockRejectedValue(new ApiError(404, 'Eleição não encontrado(a): nenhuma cadastrada'))
    render(<Resultado />)
    expect(await screen.findByRole('alert')).toHaveTextContent('nenhuma cadastrada')
  })

  it('informa quando não há candidatos para o filtro', async () => {
    mocked.resultados.apurar.mockResolvedValue({ ...resultado, totalVotos: 0, candidatos: [] })
    render(<Resultado />)
    expect(await screen.findByText('Nenhum candidato para os filtros selecionados.')).toBeInTheDocument()
  })
})
