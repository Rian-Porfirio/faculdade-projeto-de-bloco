/** Erro devolvido pela API (ou falha de conexão, com status 0). */
export class ApiError extends Error {
  readonly status: number
  readonly details: string[]

  constructor(status: number, message: string, details: string[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1'

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init.headers },
    })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar à API. Verifique se o backend está em execução.')
  }

  if (!response.ok) {
    let message = `Erro ${response.status}`
    let details: string[] = []
    try {
      const body = await response.json()
      message = body.message ?? message
      details = body.details ?? []
    } catch {
      /* resposta sem corpo JSON */
    }
    throw new ApiError(response.status, message, details)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.details.length > 0 ? `${error.message} ${error.details.join('; ')}` : error.message
  }
  return 'Ocorreu um erro inesperado.'
}
