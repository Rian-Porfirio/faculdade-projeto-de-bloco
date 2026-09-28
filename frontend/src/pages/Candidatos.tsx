import { useState } from 'react'
import type { FormEvent } from 'react'
import { api } from '../api'
import { describeError } from '../api/client'
import { CARGOS, UFS, cargoLabel } from '../api/format'
import type { Candidato, Cargo } from '../api/types'
import { ErrorNotice, Loading, SuccessNotice } from '../components/Feedback'
import { useLoad } from '../hooks'

interface FormState {
  nome: string
  numero: string
  cargo: Cargo | ''
  partidoId: string
  eleicaoId: string
  estado: string
  cidade: string
}

const VAZIO: FormState = { nome: '', numero: '', cargo: '', partidoId: '', eleicaoId: '', estado: '', cidade: '' }

export default function Candidatos() {
  const candidatos = useLoad(() => api.candidatos.listar())
  const partidos = useLoad(api.partidos.listar)
  const eleicoes = useLoad(api.eleicoes.listar)
  const [form, setForm] = useState<FormState>(VAZIO)
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [mensagem, setMensagem] = useState<string | null>(null)

  const campo = (nome: keyof FormState) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [nome]: e.target.value }))

  function editar(c: Candidato) {
    setEditandoId(c.id)
    setMensagem(null)
    setForm({
      nome: c.nome,
      numero: String(c.numero),
      cargo: c.cargo,
      partidoId: String(c.partidoId),
      eleicaoId: String(c.eleicaoId),
      estado: c.estado,
      cidade: c.cidade,
    })
  }

  function cancelar() {
    setEditandoId(null)
    setForm(VAZIO)
  }

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setMensagem(null)
    const dados = {
      nome: form.nome,
      numero: Number(form.numero),
      cargo: form.cargo as Cargo,
      partidoId: Number(form.partidoId),
      eleicaoId: Number(form.eleicaoId),
      estado: form.estado,
      cidade: form.cidade,
    }
    try {
      if (editandoId) {
        await api.candidatos.atualizar(editandoId, dados)
        setMensagem('Candidato atualizado.')
      } else {
        await api.candidatos.criar(dados)
        setMensagem('Candidato cadastrado.')
      }
      cancelar()
      candidatos.reload()
    } catch (err) {
      setErro(describeError(err))
    }
  }

  async function remover(c: Candidato) {
    if (!window.confirm(`Remover o candidato ${c.nome}?`)) return
    setErro(null)
    setMensagem(null)
    try {
      await api.candidatos.remover(c.id)
      setMensagem('Candidato removido.')
      candidatos.reload()
    } catch (err) {
      setErro(describeError(err))
    }
  }

  return (
    <>
      <h1>Candidatos</h1>
      <ErrorNotice message={erro ?? candidatos.error ?? partidos.error ?? eleicoes.error} />
      {mensagem && <SuccessNotice>{mensagem}</SuccessNotice>}

      <form className="panel form-grid" onSubmit={salvar} aria-label="Formulário de candidato">
        <h2>{editandoId ? 'Editar candidato' : 'Novo candidato'}</h2>
        <div className="field">
          <label htmlFor="c-nome">Nome</label>
          <input id="c-nome" required value={form.nome} onChange={campo('nome')} />
        </div>
        <div className="field">
          <label htmlFor="c-numero">Número</label>
          <input id="c-numero" required type="number" min={1} max={99999} value={form.numero}
            onChange={campo('numero')} />
        </div>
        <div className="field">
          <label htmlFor="c-cargo">Cargo</label>
          <select id="c-cargo" required value={form.cargo} onChange={campo('cargo')}>
            <option value="">Selecione…</option>
            {CARGOS.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="c-partido">Partido</label>
          <select id="c-partido" required value={form.partidoId} onChange={campo('partidoId')}>
            <option value="">Selecione…</option>
            {(partidos.data ?? []).map((p) => (
              <option key={p.id} value={p.id}>{p.sigla} — {p.nome}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="c-eleicao">Eleição</label>
          <select id="c-eleicao" required value={form.eleicaoId} onChange={campo('eleicaoId')}>
            <option value="">Selecione…</option>
            {(eleicoes.data ?? []).map((el) => (
              <option key={el.id} value={el.id}>{el.nome}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="c-estado">Estado</label>
          <select id="c-estado" required value={form.estado} onChange={campo('estado')}>
            <option value="">UF…</option>
            {UFS.map((uf) => (
              <option key={uf} value={uf}>{uf}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="c-cidade">Cidade</label>
          <input id="c-cidade" required value={form.cidade} onChange={campo('cidade')} />
        </div>
        <div className="actions">
          <button className="btn btn-primary" type="submit">
            {editandoId ? 'Salvar alterações' : 'Cadastrar candidato'}
          </button>
          {editandoId && (
            <button className="btn" type="button" onClick={cancelar}>
              Cancelar edição
            </button>
          )}
        </div>
      </form>

      {candidatos.loading && !candidatos.data ? (
        <Loading what="candidatos" />
      ) : (
        <table className="results">
          <thead>
            <tr>
              <th scope="col">Nº</th>
              <th scope="col">Nome</th>
              <th scope="col">Cargo</th>
              <th scope="col">Partido</th>
              <th scope="col">Local</th>
              <th scope="col">Ações</th>
            </tr>
          </thead>
          <tbody>
            {(candidatos.data ?? []).map((c) => (
              <tr key={c.id}>
                <td className="mono">{c.numero}</td>
                <td>{c.nome}</td>
                <td>{cargoLabel(c.cargo)}</td>
                <td>{c.partidoSigla}</td>
                <td>{c.cidade}/{c.estado}</td>
                <td className="row-actions">
                  <button className="btn btn-small" onClick={() => editar(c)}>Editar</button>
                  <button className="btn btn-small btn-correct" onClick={() => remover(c)}>Remover</button>
                </td>
              </tr>
            ))}
            {(candidatos.data ?? []).length === 0 && (
              <tr><td colSpan={6} className="muted">Nenhum candidato cadastrado.</td></tr>
            )}
          </tbody>
        </table>
      )}
    </>
  )
}
