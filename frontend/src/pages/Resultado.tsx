import { useState } from 'react'
import { api } from '../api'
import { CARGOS, UFS, cargoLabel, formatPercent } from '../api/format'
import type { Cargo } from '../api/types'
import { ErrorNotice, Loading } from '../components/Feedback'
import { useLoad } from '../hooks'

export default function Resultado() {
  const eleicoes = useLoad(api.eleicoes.listar)
  const [eleicaoId, setEleicaoId] = useState('')
  const [cargo, setCargo] = useState<Cargo | ''>('')
  const [estado, setEstado] = useState('')

  const resultado = useLoad(
    () => api.resultados.apurar({ eleicaoId: eleicaoId ? Number(eleicaoId) : undefined, cargo, estado }),
    [eleicaoId, cargo, estado],
  )

  const dados = resultado.data

  return (
    <>
      <h1>Resultado</h1>

      <form className="filters" onSubmit={(e) => e.preventDefault()} aria-label="Filtros">
        <div className="field">
          <label htmlFor="f-eleicao">Eleição</label>
          <select id="f-eleicao" value={eleicaoId} onChange={(e) => setEleicaoId(e.target.value)}>
            <option value="">Eleição ativa</option>
            {(eleicoes.data ?? []).map((e) => (
              <option key={e.id} value={e.id}>
                {e.nome}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="f-cargo">Cargo</label>
          <select id="f-cargo" value={cargo} onChange={(e) => setCargo(e.target.value as Cargo | '')}>
            <option value="">Todos</option>
            {CARGOS.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="f-estado">Estado do eleitor</label>
          <select id="f-estado" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {UFS.map((uf) => (
              <option key={uf} value={uf}>
                {uf}
              </option>
            ))}
          </select>
        </div>
      </form>

      <ErrorNotice message={resultado.error} />
      {resultado.loading && !dados && <Loading what="resultado" />}

      {dados && (
        <section className="panel" aria-live="polite">
          <p className="total">
            <span className="mono">{dados.totalVotos}</span> voto(s) apurados em {dados.eleicaoNome}
          </p>
          {dados.candidatos.length === 0 ? (
            <p className="muted">Nenhum candidato para os filtros selecionados.</p>
          ) : (
            <table className="results">
              <thead>
                <tr>
                  <th scope="col">Nº</th>
                  <th scope="col">Candidato</th>
                  <th scope="col">Cargo</th>
                  <th scope="col">Votos</th>
                  <th scope="col">Percentual</th>
                </tr>
              </thead>
              <tbody>
                {dados.candidatos.map((c) => (
                  <tr key={c.candidatoId}>
                    <td className="mono">{c.numero}</td>
                    <td>
                      {c.nome} <span className="muted">({c.partidoSigla})</span>
                    </td>
                    <td>{cargoLabel(c.cargo)}</td>
                    <td className="mono">{c.votos}</td>
                    <td>
                      <div className="bar" aria-hidden="true">
                        <div className="bar-fill" style={{ width: `${c.percentual}%` }} />
                      </div>
                      <span>{formatPercent(c.percentual)}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </>
  )
}
