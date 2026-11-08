import test from 'node:test'
import assert from 'node:assert/strict'
import {
  sanitizeBaseUrl,
  resolveApiBaseUrl,
  parseJsonResponse,
} from './config.ts'

test('sanitizeBaseUrl handles various inputs and strips quotes', () => {
  assert.strictEqual(sanitizeBaseUrl(undefined), '')
  assert.strictEqual(sanitizeBaseUrl(null), '')
  assert.strictEqual(sanitizeBaseUrl(''), '')
  assert.strictEqual(sanitizeBaseUrl('   '), '')
  assert.strictEqual(sanitizeBaseUrl('""'), '')
  assert.strictEqual(sanitizeBaseUrl("''"), '')
  assert.strictEqual(sanitizeBaseUrl('"http://localhost:8080"'), 'http://localhost:8080')
  assert.strictEqual(sanitizeBaseUrl("'http://localhost:8080'"), 'http://localhost:8080')
  assert.strictEqual(sanitizeBaseUrl('https://api.example.com'), 'https://api.example.com')
})

test('Production API base URL is empty by default', () => {
  const prodBase = resolveApiBaseUrl(undefined, false)
  assert.strictEqual(prodBase, '', 'Production default must be an empty string for relative paths')

  const prodEmpty = resolveApiBaseUrl('', false)
  assert.strictEqual(prodEmpty, '', 'Production empty string must remain empty')

  const prodLocalhost = resolveApiBaseUrl('http://localhost:8080', false)
  assert.strictEqual(prodLocalhost, '', 'Development fallback URL in .env must be ignored in production')

  const prodCustom = resolveApiBaseUrl('https://api.research.edu', false)
  assert.strictEqual(prodCustom, 'https://api.research.edu', 'Custom production base URL is preserved')
})

test('Development API base URL defaults to http://localhost:8080', () => {
  const devDefault = resolveApiBaseUrl(undefined, true)
  assert.strictEqual(devDefault, 'http://localhost:8080')

  const devEmpty = resolveApiBaseUrl('', true)
  assert.strictEqual(devEmpty, 'http://localhost:8080')
})

test('No generated production API URL contains literal quote characters', () => {
  // Simulating accidental Docker Compose build-arg quoting e.g. VITE_API_BASE_URL=""
  const prodQuoted = resolveApiBaseUrl('""', false)
  assert.strictEqual(prodQuoted, '')
  assert.doesNotMatch(prodQuoted, /["']/)

  const devQuoted = resolveApiBaseUrl('""', true)
  assert.strictEqual(devQuoted, 'http://localhost:8080')
  assert.doesNotMatch(devQuoted, /["']/)

  // Verify constructed URL path never contains /""/
  const projectId = 1
  const constructedPath = `${prodQuoted}/api/projects/${projectId}/dashboard`
  assert.strictEqual(constructedPath, '/api/projects/1/dashboard')
  assert.doesNotMatch(constructedPath, /["']/)
})

test('parseJsonResponse successfully parses valid application/json responses', async () => {
  const mockPayload = { id: 1, name: 'Test' }
  const mockResponse = new Response(JSON.stringify(mockPayload), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })

  const result = await parseJsonResponse<typeof mockPayload>(mockResponse)
  assert.deepStrictEqual(result, mockPayload)
})

test('Non-JSON API responses produce a useful error rather than an unexplained JSON parsing exception', async () => {
  const htmlDoc = '<!doctype html><html><body><div id="root"></div></body></html>'
  const htmlResponse = new Response(htmlDoc, {
    status: 200,
    headers: { 'Content-Type': 'text/html; charset=utf-8' },
  })

  await assert.rejects(
    async () => {
      await parseJsonResponse(htmlResponse, 'Could not load project dashboard')
    },
    (err: Error) => {
      assert(err.message.includes('Could not load project dashboard'))
      assert(err.message.includes('expected "application/json" response'))
      assert(err.message.includes('HTML document returned'))
      assert(!err.message.includes("Unexpected token '<'"))
      return true
    },
  )
})
