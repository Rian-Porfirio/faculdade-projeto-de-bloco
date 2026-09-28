import type { Cargo } from './types'

export const CARGOS: { value: Cargo; label: string }[] = [
  { value: 'PRESIDENTE', label: 'Presidente' },
  { value: 'GOVERNADOR', label: 'Governador' },
  { value: 'SENADOR', label: 'Senador' },
  { value: 'DEPUTADO_FEDERAL', label: 'Deputado federal' },
  { value: 'DEPUTADO_ESTADUAL', label: 'Deputado estadual' },
  { value: 'PREFEITO', label: 'Prefeito' },
  { value: 'VEREADOR', label: 'Vereador' },
]

export const UFS = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA',
  'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
]

export const cargoLabel = (cargo: string): string =>
  CARGOS.find((c) => c.value === cargo)?.label ?? cargo

export const formatDateTime = (iso: string): string =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })

export const formatPercent = (value: number): string =>
  `${value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 2 })}%`
