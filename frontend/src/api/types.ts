export type Cargo =
  | 'PRESIDENTE'
  | 'GOVERNADOR'
  | 'SENADOR'
  | 'DEPUTADO_FEDERAL'
  | 'DEPUTADO_ESTADUAL'
  | 'PREFEITO'
  | 'VEREADOR'

export type StatusEleicao = 'AGENDADA' | 'ATIVA' | 'ENCERRADA'

export interface Partido {
  id: number
  sigla: string
  nome: string
  numero: number
}

export interface LocalVotacao {
  id: number
  nome: string
  cidade: string
  estado: string
  zona: string
}

export interface Eleicao {
  id: number
  nome: string
  descricao?: string
  dataInicio: string
  dataTermino: string
  status: StatusEleicao
}

export interface Eleitor {
  id: number
  nome: string
  identificador: string
  estado: string
  cidade: string
  localVotacaoId: number
  localVotacaoNome: string
}

export interface EleitorInput {
  nome: string
  identificador: string
  estado: string
  cidade: string
  localVotacaoId: number
}

export interface Candidato {
  id: number
  nome: string
  numero: number
  cargo: Cargo
  partidoId: number
  partidoSigla: string
  eleicaoId: number
  estado: string
  cidade: string
  regiao: string
}

export interface CandidatoInput {
  nome: string
  numero: number
  cargo: Cargo
  partidoId: number
  eleicaoId: number
  estado: string
  cidade: string
}

export interface Voto {
  id: number
  eleitorId: number
  eleitorNome: string
  candidatoId: number
  candidatoNome: string
  candidatoNumero: number
  eleicaoId: number
  localVotacaoId: number
  dataHora: string
}

export interface VotoInput {
  eleitorId: number
  candidatoId: number
  eleicaoId: number
}

export interface ResultadoCandidato {
  candidatoId: number
  nome: string
  numero: number
  partidoSigla: string
  cargo: Cargo
  votos: number
  percentual: number
}

export interface Resultado {
  eleicaoId: number
  eleicaoNome: string
  cargo: Cargo | null
  estado: string | null
  totalVotos: number
  candidatos: ResultadoCandidato[]
  /** Preenchido apenas pelo result-service (Branch 2). */
  fonte?: string
}

export interface EventoAuditoria {
  id: number
  eventId: string
  eventType: string
  routingKey: string | null
  occurredAt: string | null
  recebidoEm: string
  payload: string | null
}

export interface ResumoTipo {
  tipo: string
  total: number
}

/** De onde vêm os resultados: consulta síncrona ao voting-service ou projeção alimentada por eventos. */
export type FonteResultado = 'voting' | 'eventos'
