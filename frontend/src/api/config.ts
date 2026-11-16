/**
 * Sanitizes an API base URL string by trimming whitespace and stripping
 * leading/trailing quotes (e.g. from accidental Docker build-arg quoting).
 */
export function sanitizeBaseUrl(url: unknown): string {
  if (typeof url !== 'string') {
    return ''
  }
  const trimmed = url.trim()
  if (trimmed.length === 0) {
    return ''
  }
  // Strip enclosing quotes (double, single, or escaped)
  return trimmed.replace(/^["']+|["']+$/g, '').trim()
}

/**
 * Resolves the API base URL based on the environment configuration.
 * - Development: defaults to 'http://localhost:8080' when no explicit base URL is provided.
 * - Production: defaults to '' (empty string) so browser requests use relative '/api/...' paths.
 *   If the local dev URL ('http://localhost:8080') was present in a base .env file, it is ignored
 *   in production so relative container routing is preserved.
 */
export function resolveApiBaseUrl(envBaseUrl: unknown, isDev: boolean): string {
  const sanitized = sanitizeBaseUrl(envBaseUrl)
  if (isDev) {
    return sanitized.length > 0 ? sanitized : 'http://localhost:8080'
  }
  // In production:
  if (
    sanitized.length > 0 &&
    sanitized !== 'http://localhost:8080' &&
    sanitized !== 'http://127.0.0.1:8080'
  ) {
    return sanitized
  }
  return ''
}

/**
 * Helper to parse a fetch Response as JSON with content-type verification.
 * Prevents cryptic "Unexpected token '<', '<!doctype '... is not valid JSON" errors
 * when an SPA HTML page is returned by Nginx instead of an API JSON endpoint.
 */
export async function parseJsonResponse<T>(
  response: Response,
  contextMessage = 'Failed to parse JSON response',
): Promise<T> {
  const contentType = response.headers?.get?.('content-type') || ''
  if (!contentType.includes('application/json')) {
    let preview = ''
    try {
      const text = await response.text()
      preview = text.slice(0, 100).replace(/\s+/g, ' ').trim()
    } catch {
      // Ignore text extraction errors
    }
    const isHtml = preview.toLowerCase().startsWith('<!doctype') || preview.toLowerCase().startsWith('<html')
    throw new Error(
      `${contextMessage}: expected "application/json" response, but received "${contentType || 'unknown'}"${
        isHtml ? ' (HTML document returned instead of API JSON)' : ''
      }${preview ? ` [preview: ${preview.slice(0, 60)}]` : ''}`,
    )
  }

  try {
    return (await response.json()) as T
  } catch (error) {
    throw new Error(
      `${contextMessage}: invalid JSON body (${error instanceof Error ? error.message : String(error)})`,
    )
  }
}

/**
 * Builds the message for a failed API response. Uses the backend's `message` field when the
 * error body carries one, otherwise falls back to the HTTP status code.
 */
export async function readErrorMessage(response: Response, contextMessage: string): Promise<string> {
  let reason = ''
  const contentType = response.headers?.get?.('content-type') || ''
  if (contentType.includes('json')) {
    try {
      const body = (await response.json()) as { message?: unknown }
      if (typeof body?.message === 'string') {
        reason = body.message.trim()
      }
    } catch {
      // Unreadable error body: fall back to the status code
    }
  }
  return reason ? `${contextMessage}: ${reason}` : `${contextMessage} (${response.status})`
}

// Safely access Vite import.meta.env
const metaEnv = typeof import.meta !== 'undefined' && 'env' in import.meta
  ? (import.meta as { env?: Record<string, string | boolean | undefined> }).env
  : undefined

export const API_BASE_URL = resolveApiBaseUrl(
  metaEnv?.VITE_API_BASE_URL,
  Boolean(metaEnv?.DEV),
)
