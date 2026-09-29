import { Link } from 'react-router-dom'
import { api } from '../api'
import { formatPercent } from '../api/format'
import { ErrorNotice, Loading } from '../components/Feedback'
import { useLoad } from '../hooks'

export default function Dashboard() {
  const resultado = useLoad(() => api.resultados.apurar())
  const candidatos = useLoad(() => api.candidatos.listar())
  const eleitores = useLoad(() => api.eleitores.listar())

  const error = resultado.error ?? candidatos.error ?? eleitores.error
  if (resultado.loading || candidatos.loading || eleitores.loading) return <Loading />

  const lider = resultado.data?.candidatos[0]

  return (
    <>
      <h1>{resultado.data?.eleicaoNome ?? 'Sistema de votação'}</h1>
      <ErrorNotice message={error} />

      <section className="stats" aria-label="Resumo">
        <div className="stat">
          <span className="stat-value mono">{resultado.data?.totalVotos ?? 0}</span>
          <span className="stat-label">votos registrados</span>
        </div>
        <div className="stat">
          <span className="stat-value mono">{candidatos.data?.length ?? 0}</span>
          <span className="stat-label">candidatos</span>
        </div>
        <div className="stat">
          <span className="stat-value mono">{eleitores.data?.length ?? 0}</span>
          <span className="stat-label">eleitores</span>
        </div>
      </section>

      <section className="panel">
        <h2>Em primeiro lugar</h2>
        {lider && lider.votos > 0 ? (
          <p>
            <strong>{lider.nome}</strong> ({lider.partidoSigla}) tem {lider.votos} voto(s),{' '}
            {formatPercent(lider.percentual)} do total.
          </p>
        ) : (
          <p className="muted">Nenhum voto registrado ainda.</p>
        )}
        <p className="actions">
          <Link className="btn btn-primary" to="/votacao">
            Ir para a votação
          </Link>
          <Link className="btn" to="/resultado">
            Ver resultado completo
          </Link>
        </p>
      </section>
    </>
  )
}
