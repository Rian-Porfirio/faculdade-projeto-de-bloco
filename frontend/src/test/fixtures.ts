import type { Candidato, Eleicao, Eleitor, Resultado } from '../api/types'

export const eleicaoAtiva: Eleicao = {
  id: 1, nome: 'Eleição Simulada 2026', dataInicio: '2026-01-01T08:00:00', dataTermino: '2026-12-31T17:00:00', status: 'ATIVA',
}

export const eleitorMaria: Eleitor = {
  id: 1, nome: 'Maria Silva', identificador: '100000000001', estado: 'SP', cidade: 'São Paulo',
  localVotacaoId: 1, localVotacaoNome: 'Escola Municipal Centro',
}

export const candidatoAna: Candidato = {
  id: 10, nome: 'Ana Ribeiro', numero: 10, cargo: 'PRESIDENTE', partidoId: 1, partidoSigla: 'AZL',
  eleicaoId: 1, estado: 'SP', cidade: 'São Paulo', regiao: 'SUDESTE',
}

export const candidatoBruno: Candidato = { ...candidatoAna, id: 11, nome: 'Bruno Tavares', numero: 20, partidoSigla: 'VRD' }

export const resultado: Resultado = {
  eleicaoId: 1, eleicaoNome: 'Eleição Simulada 2026', cargo: null, estado: null, totalVotos: 4,
  candidatos: [
    { candidatoId: 10, nome: 'Ana Ribeiro', numero: 10, partidoSigla: 'AZL', cargo: 'PRESIDENTE', votos: 3, percentual: 75 },
    { candidatoId: 11, nome: 'Bruno Tavares', numero: 20, partidoSigla: 'VRD', cargo: 'PRESIDENTE', votos: 1, percentual: 25 },
  ],
}
