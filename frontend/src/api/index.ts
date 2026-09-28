import { request } from './client'
import type {
  Candidato,
  CandidatoInput,
  Cargo,
  Eleicao,
  Eleitor,
  EleitorInput,
  LocalVotacao,
  Partido,
  Resultado,
  ResultadoCandidato,
  Voto,
  VotoInput,
} from './types'

const json = (body: unknown) => JSON.stringify(body)

function query(params: Record<string, string | number | undefined | null>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const api = {
  partidos: {
    listar: () => request<Partido[]>('/partidos'),
  },
  locais: {
    listar: () => request<LocalVotacao[]>('/locais-votacao'),
  },
  eleicoes: {
    listar: () => request<Eleicao[]>('/eleicoes'),
  },
  eleitores: {
    listar: () => request<Eleitor[]>('/eleitores'),
    criar: (dados: EleitorInput) => request<Eleitor>('/eleitores', { method: 'POST', body: json(dados) }),
    atualizar: (id: number, dados: EleitorInput) =>
      request<Eleitor>(`/eleitores/${id}`, { method: 'PUT', body: json(dados) }),
    remover: (id: number) => request<void>(`/eleitores/${id}`, { method: 'DELETE' }),
    votos: (id: number) => request<Voto[]>(`/eleitores/${id}/votos`),
  },
  candidatos: {
    listar: (eleicaoId?: number) => request<Candidato[]>(`/candidatos${query({ eleicaoId })}`),
    criar: (dados: CandidatoInput) => request<Candidato>('/candidatos', { method: 'POST', body: json(dados) }),
    atualizar: (id: number, dados: CandidatoInput) =>
      request<Candidato>(`/candidatos/${id}`, { method: 'PUT', body: json(dados) }),
    remover: (id: number) => request<void>(`/candidatos/${id}`, { method: 'DELETE' }),
    resultado: (id: number) => request<ResultadoCandidato>(`/candidatos/${id}/resultado`),
  },
  votos: {
    registrar: (dados: VotoInput) => request<Voto>('/votos', { method: 'POST', body: json(dados) }),
    listar: (eleicaoId?: number) => request<Voto[]>(`/votos${query({ eleicaoId })}`),
  },
  resultados: {
    apurar: (filtros: { eleicaoId?: number; cargo?: Cargo | ''; estado?: string } = {}) =>
      request<Resultado>(`/resultados${query(filtros)}`),
  },
}
