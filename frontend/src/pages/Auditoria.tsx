import { useState } from 'react'
import { api } from '../api'
import { formatDateTime } from '../api/format'
import { ErrorNotice, Loading } from '../components/Feedback'
import { useLoad } from '../hooks'

export default function Auditoria() {
  const [tipo, setTipo] = useState('')
  const resumo = useLoad(api.auditoria.resumo)
  const eventos = useLoad(() => api.auditoria.listar(tipo || undefined), [tipo])

  function atualizar() {
    resumo.reload()
    eventos.reload()
  }

  return (
    <>
      <h1>Auditoria de eventos</h1>
      <p className="muted">
        Cada ação do sistema publica um evento no RabbitMQ. O audit-service recebe todos e os registra aqui.
      </p>
      <ErrorNotice message={resumo.error ?? eventos.error} />

      <section className="stats" aria-label="Eventos por tipo">
        {(resumo.data ?? []).map((r) => (
          <button
            key={r.tipo}
            type="button"
            className="stat stat-button"
            aria-pressed={tipo === r.tipo}
            onClick={() => setTipo(tipo === r.tipo ? '' : r.tipo)}
          >
            <span className="stat-value mono">{r.total}</span>
            <span className="stat-label">{r.tipo}</span>
          </button>
        ))}
        {resumo.data?.length === 0 && <p className="muted">Nenhum evento recebido ainda.</p>}
      </section>

      <p className="actions">
        <button className="btn" onClick={atualizar}>
          Atualizar
        </button>
        {tipo && (
          <button className="btn" onClick={() => setTipo('')}>
            Limpar filtro ({tipo})
          </button>
        )}
      </p>

      {eventos.loading && !eventos.data ? (
        <Loading what="eventos" />
      ) : (
        <table className="results">
          <thead>
            <tr>
              <th scope="col">Recebido em</th>
              <th scope="col">Tipo</th>
              <th scope="col">Routing key</th>
              <th scope="col">Evento</th>
            </tr>
          </thead>
          <tbody>
            {(eventos.data ?? []).map((e) => (
              <tr key={e.id}>
                <td>{formatDateTime(e.recebidoEm)}</td>
                <td>{e.eventType}</td>
                <td className="mono">{e.routingKey}</td>
                <td className="mono">{e.eventId.slice(0, 8)}</td>
              </tr>
            ))}
            {(eventos.data ?? []).length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  Nenhum evento para exibir.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </>
  )
}
