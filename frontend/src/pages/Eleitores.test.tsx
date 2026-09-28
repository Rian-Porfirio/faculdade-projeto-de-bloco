import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { eleitorMaria } from '../test/fixtures'
import Eleitores from './Eleitores'

vi.mock('../api', () => ({
  api: {
    eleitores: { listar: vi.fn(), criar: vi.fn(), atualizar: vi.fn(), remover: vi.fn(), votos: vi.fn() },
    locais: { listar: vi.fn() },
  },
}))
import { api } from '../api'

const mocked = vi.mocked(api, true)

beforeEach(() => {
  vi.clearAllMocks()
  mocked.eleitores.listar.mockResolvedValue([eleitorMaria])
  mocked.locais.listar.mockResolvedValue([{ id: 1, nome: 'Escola Municipal Centro', cidade: 'São Paulo', estado: 'SP', zona: '001' }])
})

describe('Eleitores', () => {
  it('lista eleitores', async () => {
    render(<Eleitores />)
    expect(await screen.findByText('Maria Silva')).toBeInTheDocument()
  })

  it('cadastra eleitor', async () => {
    const user = userEvent.setup()
    mocked.eleitores.criar.mockResolvedValue(eleitorMaria)
    render(<Eleitores />)
    await screen.findByText('Maria Silva')

    await user.type(screen.getByLabelText('Nome'), 'João Paulo')
    await user.type(screen.getByLabelText('Identificador'), '555')
    await user.selectOptions(screen.getByLabelText('Estado'), 'PR')
    await user.type(screen.getByLabelText('Cidade'), 'Curitiba')
    await user.selectOptions(screen.getByLabelText('Local de votação'), '1')
    await user.click(screen.getByRole('button', { name: 'Cadastrar eleitor' }))

    expect(mocked.eleitores.criar).toHaveBeenCalledWith({
      nome: 'João Paulo', identificador: '555', estado: 'PR', cidade: 'Curitiba', localVotacaoId: 1,
    })
  })

  it('mostra os votos de um eleitor', async () => {
    const user = userEvent.setup()
    mocked.eleitores.votos.mockResolvedValue([])
    render(<Eleitores />)
    await screen.findByText('Maria Silva')

    await user.click(screen.getByRole('button', { name: 'Votos' }))

    expect(await screen.findByText('Este eleitor ainda não votou.')).toBeInTheDocument()
  })

  it('exibe erro ao tentar remover eleitor que já votou', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mocked.eleitores.remover.mockRejectedValue(new ApiError(409, 'Não é possível remover um eleitor que já votou.'))
    render(<Eleitores />)
    await screen.findByText('Maria Silva')

    await user.click(screen.getByRole('button', { name: 'Remover' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('já votou')
  })
})
