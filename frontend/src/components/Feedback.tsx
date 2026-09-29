import type { ReactNode } from 'react'

export function ErrorNotice({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="notice notice-error">
      {message}
    </p>
  )
}

export function SuccessNotice({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="notice notice-ok">
      {children}
    </p>
  )
}

export function Loading({ what = 'dados' }: { what?: string }) {
  return <p className="muted">Carregando {what}…</p>
}
