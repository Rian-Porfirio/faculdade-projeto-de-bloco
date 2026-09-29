import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { candidatoAna, eleicaoAtiva } from '../test/fixtures'
import Candidatos from './Candidatos'

vi.mock('../api', () => ({
  api: {
    candidatos: { listar: vi.fn(), criar: vi.fn(), atualizar: vi.fn(), remover: vi.fn() },
    partidos: { listar: vi.fn() },
    eleicoes: { listar: vi.fn() },
  },
}))
import { api } from '../api'

const mocked = vi.mocked(api, true)

beforeEach(() => {
  vi.clearAllMocks()
  mocked.candidatos.listar.mockResolvedValue([candidatoAna])
  mocked.partidos.listar.mockResolvedValue([{ id: 1, sigla: 'AZL', nome: 'Partido Azul', numero: 10 }])
  mocked.eleicoes.listar.mockResolvedValue([eleicaoAtiva])
})

async function preencher(user: ReturnType<typeof userEvent.setup>) {
  await screen.findByText('Ana Ribeiro')
  await user.type(screen.getByLabelText('Nome'), 'Carla Mendes')
  await user.type(screen.getByLabelText('Número'), '30')
  await user.selectOptions(screen.getByLabelText('Cargo'), 'PRESIDENTE')
  await user.selectOptions(screen.getByLabelText('Partido'), '1')
  await user.selectOptions(screen.getByLabelText('Eleição'), '1')
  await user.selectOptions(screen.getByLabelText('Estado'), 'SP')
  await user.type(screen.getByLabelText('Cidade'), 'Campinas')
}

describe('Candidatos', () => {
  it('lista os candidatos cadastrados', async () => {
    render(<Candidatos />)
    expect(await screen.findByText('Ana Ribeiro')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Presidente')).toBeInTheDocument()
  })

  it('cadastra um candidato enviando os dados convertidos', async () => {
    const user = userEvent.setup()
    mocked.candidatos.criar.mockResolvedValue({ ...candidatoAna, id: 12 })
    render(<Candidatos />)

    await preencher(user)
    await user.click(screen.getByRole('button', { name: 'Cadastrar candidato' }))

    expect(mocked.candidatos.criar).toHaveBeenCalledWith({
      nome: 'Carla Mendes', numero: 30, cargo: 'PRESIDENTE', partidoId: 1, eleicaoId: 1, estado: 'SP', cidade: 'Campinas',
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Candidato cadastrado.')
  })

  it('mostra o erro devolvido pela API ao cadastrar', async () => {
    const user = userEvent.setup()
    mocked.candidatos.criar.mockRejectedValue(new ApiError(409, 'Já existe candidato com esse número.'))
    render(<Candidatos />)

    await preencher(user)
    await user.click(screen.getByRole('button', { name: 'Cadastrar candidato' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe candidato com esse número.')
  })

  it('remove um candidato após confirmação', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mocked.candidatos.remover.mockResolvedValue(undefined)
    render(<Candidatos />)

    await screen.findByText('Ana Ribeiro')
    await user.click(screen.getByRole('button', { name: 'Remover' }))

    expect(mocked.candidatos.remover).toHaveBeenCalledWith(10)
  })
})
