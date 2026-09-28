import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import { describeError } from '../api/client'
import { UFS, formatDateTime } from '../api/format'
import type { Eleitor, Voto } from '../api/types'
import { ErrorNotice, Loading, SuccessNotice } from '../components/Feedback'
import { useLoad } from '../hooks'

interface FormState {
  nome: string
  identificador: string
  estado: string
  cidade: string
  localVotacaoId: string
}

const VAZIO: FormState = { nome: '', identificador: '', estado: '', cidade: '', localVotacaoId: '' }

export default function Eleitores() {
  const eleitores = useLoad(api.eleitores.listar)
  const locais = useLoad(api.locais.listar)
  const [form, setForm] = useState<FormState>(VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [historico, setHistorico] = useState<{ eleitor: Eleitor; votos: Voto[] } | null>(null)

  const campo = (nome: keyof FormState) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [nome]: e.target.value }))

  function editar(e: Eleitor) {
    setEditandoId(e.id)
    setMensagem(null)
    setForm({
      nome: e.nome,
      identificador: e.identificador,
      estado: e.estado,
      cidade: e.cidade,
      localVotacaoId: String(e.localVotacaoId),
    })
  }

  function cancelar() {
    setEditandoId(null)
    setForm(VAZIO)
  }

  async function salvar(ev: FormEvent) {
    ev.preventDefault()
    setErro(null)
    setMensagem(null)
    const dados = { ...form, localVotacaoId: Number(form.localVotacaoId) }
    try {
      if (editandoId) {
        await api.eleitores.atualizar(editandoId, dados)
        setMensagem('Eleitor atualizado.')
      } else {
        await api.eleitores.criar(dados)
        setMensagem('Eleitor cadastrado.')
      }
      cancelar()
      eleitores.reload()
    } catch (err) {
      setErro(describeError(err))
    }
  }

  async function remover(e: Eleitor) {
    if (!window.confirm(`Remover o eleitor ${e.nome}?`)) return
    setErro(null)
    setMensagem(null)
    try {
      await api.eleitores.remover(e.id)
      setMensagem('Eleitor removido.')
      eleitores.reload()
    } catch (err) {
      setErro(describeError(err))
    }
  }

  async function verVotos(e: Eleitor) {
    setErro(null)
    try {
      setHistorico({ eleitor: e, votos: await api.eleitores.votos(e.id) })
    } catch (err) {
      setErro(describeError(err))
    }
  }

  return (
    <>
      <h1>Eleitores</h1>
      <ErrorNotice message={erro ?? eleitores.error ?? locais.error} />
      {mensagem && <SuccessNotice>{mensagem}</SuccessNotice>}

      <form className="panel form-grid" onSubmit={salvar} aria-label="Formulário de eleitor">
        <h2>{editandoId ? 'Editar eleitor' : 'Novo eleitor'}</h2>
        <div className="field">
          <label htmlFor="e-nome">Nome</label>
          <input id="e-nome" required value={form.nome} onChange={campo('nome')} />
        </div>
        <div className="field">
          <label htmlFor="e-ident">Identificador</label>
          <input id="e-ident" required maxLength={20} value={form.identificador}
            onChange={campo('identificador')} />
        </div>
        <div className="field">
          <label htmlFor="e-estado">Estado</label>
          <select id="e-estado" required value={form.estado} onChange={campo('estado')}>
            <option value="">UF…</option>
            {UFS.map((uf) => (
              <option key={uf} value={uf}>{uf}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="e-cidade">Cidade</label>
          <input id="e-cidade" required value={form.cidade} onChange={campo('cidade')} />
        </div>
        <div className="field">
          <label htmlFor="e-local">Local de votação</label>
          <select id="e-local" required value={form.localVotacaoId} onChange={campo('localVotacaoId')}>
            <option value="">Selecione…</option>
            {(locais.data ?? []).map((l) => (
              <option key={l.id} value={l.id}>{l.nome} (zona {l.zona})</option>
            ))}
          </select>
        </div>
        <div className="actions">
          <button className="btn btn-primary" type="submit">
            {editandoId ? 'Salvar alterações' : 'Cadastrar eleitor'}
          </button>
          {editandoId && (
            <button className="btn" type="button" onClick={cancelar}>
              Cancelar edição
            </button>
          )}
        </div>
      </form>

      {eleitores.loading && !eleitores.data ? (
        <Loading what="eleitores" />
      ) : (
        <table className="results">
          <thead>
            <tr>
              <th scope="col">Nome</th>
              <th scope="col">Identificador</th>
              <th scope="col">Cidade/UF</th>
              <th scope="col">Local de votação</th>
              <th scope="col">Ações</th>
            </tr>
          </thead>
          <tbody>
            {(eleitores.data ?? []).map((e) => (
              <tr key={e.id}>
                <td>{e.nome}</td>
                <td className="mono">{e.identificador}</td>
                <td>{e.cidade}/{e.estado}</td>
                <td>{e.localVotacaoNome}</td>
                <td className="row-actions">
                  <button className="btn btn-small" onClick={() => verVotos(e)}>Votos</button>
                  <button className="btn btn-small" onClick={() => editar(e)}>Editar</button>
                  <button className="btn btn-small btn-correct" onClick={() => remover(e)}>Remover</button>
                </td>
              </tr>
            ))}
            {(eleitores.data ?? []).length === 0 && (
              <tr><td colSpan={5} className="muted">Nenhum eleitor cadastrado.</td></tr>
            )}
          </tbody>
        </table>
      )}

      {historico && (
        <section className="panel" aria-label="Votos do eleitor">
          <h2>Votos de {historico.eleitor.nome}</h2>
          {historico.votos.length === 0 ? (
            <p className="muted">Este eleitor ainda não votou.</p>
          ) : (
            <ul>
              {historico.votos.map((v) => (
                <li key={v.id}>
                  {v.candidatoNome} (<span className="mono">{v.candidatoNumero}</span>) em{' '}
                  {formatDateTime(v.dataHora)}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </>
  )
}
