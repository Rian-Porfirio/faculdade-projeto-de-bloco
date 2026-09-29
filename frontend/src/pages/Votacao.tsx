import { useMemo, useState } from 'react'
import { api } from '../api'
import { describeError } from '../api/client'
import { cargoLabel, formatDateTime } from '../api/format'
import type { Candidato, Voto } from '../api/types'
import { ErrorNotice, Loading } from '../components/Feedback'
import { useLoad } from '../hooks'

export default function Votacao() {
  const eleicoes = useLoad(api.eleicoes.listar)
  const eleitores = useLoad(api.eleitores.listar)
  const [eleicaoEscolhida, setEleicaoEscolhida] = useState<number | null>(null)
  const [eleitorId, setEleitorId] = useState('')
  const [candidato, setCandidato] = useState<Candidato | null>(null)
  const [comprovante, setComprovante] = useState<Voto | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  const ativas = useMemo(() => (eleicoes.data ?? []).filter((e) => e.status === 'ATIVA'), [eleicoes.data])
  const eleicaoId = eleicaoEscolhida ?? ativas[0]?.id ?? null

  const candidatos = useLoad(
    () => (eleicaoId ? api.candidatos.listar(eleicaoId) : Promise.resolve([] as Candidato[])),
    [eleicaoId],
  )

  const porCargo = useMemo(() => {
    const grupos = new Map<string, Candidato[]>()
    ;(candidatos.data ?? []).forEach((c) => grupos.set(c.cargo, [...(grupos.get(c.cargo) ?? []), c]))
    return [...grupos.entries()]
  }, [candidatos.data])

  const eleitorSelecionado = eleitores.data?.find((e) => String(e.id) === eleitorId)

  async function confirmar() {
    if (!eleicaoId || !eleitorSelecionado || !candidato) return
    setEnviando(true)
    setErro(null)
    try {
      const voto = await api.votos.registrar({
        eleitorId: eleitorSelecionado.id,
        candidatoId: candidato.id,
        eleicaoId,
      })
      setComprovante(voto)
    } catch (e) {
      setErro(describeError(e))
    } finally {
      setEnviando(false)
    }
  }

  function novoVoto() {
    setComprovante(null)
    setCandidato(null)
    setEleitorId('')
    setErro(null)
  }

  if (eleicoes.loading || eleitores.loading) return <Loading />

  if (comprovante) {
    return (
      <>
        <h1>Votação</h1>
        <section className="panel receipt" aria-live="polite">
          <h2>Voto registrado</h2>
          <dl className="kv">
            <dt>Eleitor</dt>
            <dd>{comprovante.eleitorNome}</dd>
            <dt>Candidato</dt>
            <dd>
              <span className="mono">{comprovante.candidatoNumero}</span> {comprovante.candidatoNome}
            </dd>
            <dt>Data e hora</dt>
            <dd>{formatDateTime(comprovante.dataHora)}</dd>
            <dt>Comprovante</dt>
            <dd className="mono">#{comprovante.id}</dd>
          </dl>
          <button className="btn btn-primary" onClick={novoVoto}>
            Registrar outro voto
          </button>
        </section>
      </>
    )
  }

  return (
    <>
      <h1>Votação</h1>
      <ErrorNotice message={eleicoes.error ?? eleitores.error ?? candidatos.error} />

      {ativas.length === 0 ? (
        <p className="notice">Não há eleição ativa no momento.</p>
      ) : (
        <div className="ballot">
          <section className="panel">
            <h2>1. Identifique o eleitor</h2>
            <div className="field">
              <label htmlFor="eleicao">Eleição</label>
              <select
                id="eleicao"
                value={eleicaoId ?? ''}
                onChange={(e) => {
                  setEleicaoEscolhida(Number(e.target.value))
                  setCandidato(null)
                }}
              >
                {ativas.map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="eleitor">Eleitor</label>
              <select id="eleitor" value={eleitorId} onChange={(e) => setEleitorId(e.target.value)}>
                <option value="">Selecione…</option>
                {(eleitores.data ?? []).map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.nome} ({e.identificador})
                  </option>
                ))}
              </select>
            </div>
            {eleitorSelecionado && (
              <p className="muted">
                Local de votação: {eleitorSelecionado.localVotacaoNome}, {eleitorSelecionado.cidade}/
                {eleitorSelecionado.estado}
              </p>
            )}
          </section>

          <section className="panel">
            <h2>2. Escolha o candidato</h2>
            {candidatos.loading && <Loading what="candidatos" />}
            {!candidatos.loading && porCargo.length === 0 && (
              <p className="muted">Nenhum candidato cadastrado para esta eleição.</p>
            )}
            {porCargo.map(([cargo, lista]) => (
              <div key={cargo} className="cargo-group">
                <h3>{cargoLabel(cargo)}</h3>
                <ul className="candidate-list">
                  {lista.map((c) => (
                    <li key={c.id}>
                      <button
                        type="button"
                        className="candidate"
                        aria-pressed={candidato?.id === c.id}
                        onClick={() => setCandidato(c)}
                      >
                        <span className="candidate-number mono">{c.numero}</span>
                        <span className="candidate-name">{c.nome}</span>
                        <span className="muted">
                          {c.partidoSigla} · {c.cidade}/{c.estado}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </section>

          <section className="panel confirm">
            <h2>3. Confirme</h2>
            {candidato && eleitorSelecionado ? (
              <p>
                {eleitorSelecionado.nome} votará em{' '}
                <strong>
                  <span className="mono">{candidato.numero}</span> {candidato.nome}
                </strong>
                .
              </p>
            ) : (
              <p className="muted">Selecione o eleitor e o candidato para continuar.</p>
            )}
            <ErrorNotice message={erro} />
            <div className="actions">
              <button
                className="btn btn-confirm"
                disabled={!candidato || !eleitorSelecionado || enviando}
                onClick={confirmar}
              >
                {enviando ? 'Enviando…' : 'Confirmar voto'}
              </button>
              <button
                className="btn btn-correct"
                disabled={!candidato || enviando}
                onClick={() => setCandidato(null)}
              >
                Corrigir escolha
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  )
}
