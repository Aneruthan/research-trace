import test from 'node:test'
import assert from 'node:assert/strict'
import { archiveProject, createProject, getProjects, updateProject } from './projects.ts'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

test('project list loading requests every project', async () => {
  const originalFetch = globalThis.fetch
  let url = ''
  globalThis.fetch = (async (input: RequestInfo | URL) => {
    url = String(input)
    return jsonResponse([
      { id: 1, name: 'Multimodal Biomedical AI', status: 'ACTIVE' },
      { id: 2, name: 'GreenRoute', status: 'ACTIVE' },
    ])
  }) as typeof fetch
  try {
    const projects = await getProjects()
    assert.match(url, /\/api\/projects$/)
    assert.equal(projects.length, 2)
    assert.equal(projects[1]?.name, 'GreenRoute')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('create project form posts the profile without invented dates', async () => {
  const originalFetch = globalThis.fetch
  let body = ''
  globalThis.fetch = (async (_input: RequestInfo | URL, init?: RequestInit) => {
    body = String(init?.body ?? '')
    return jsonResponse({ id: 3, name: 'Crop Disease Detection', status: 'ACTIVE' }, 201)
  }) as typeof fetch
  try {
    const created = await createProject({
      name: 'Crop Disease Detection',
      description: null,
      objectives: 'Detect leaf disease',
      startDate: null,
      targetDate: null,
      supervisorId: null,
    })
    const parsed = JSON.parse(body) as { startDate: null; supervisorId: null }
    assert.equal(parsed.startDate, null)
    assert.equal(parsed.supervisorId, null)
    assert.equal(created.id, 3)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('edit project form puts the existing project id', async () => {
  const originalFetch = globalThis.fetch
  let url = ''
  let method = ''
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    url = String(input)
    method = init?.method ?? 'GET'
    return jsonResponse({ id: 2, name: 'GreenRoute revised', status: 'ACTIVE' })
  }) as typeof fetch
  try {
    const updated = await updateProject(2, {
      name: 'GreenRoute revised',
      description: null,
      objectives: null,
      startDate: null,
      targetDate: null,
      supervisorId: null,
    })
    assert.equal(method, 'PUT')
    assert.match(url, /\/api\/projects\/2$/)
    assert.equal(updated.name, 'GreenRoute revised')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('archive confirmation posts to the archive endpoint', async () => {
  const originalFetch = globalThis.fetch
  let url = ''
  let method = ''
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    url = String(input)
    method = init?.method ?? 'GET'
    return jsonResponse({ id: 2, name: 'GreenRoute', status: 'ARCHIVED' })
  }) as typeof fetch
  try {
    const archived = await archiveProject(2)
    assert.equal(method, 'POST')
    assert.match(url, /\/api\/projects\/2\/archive$/)
    assert.equal(archived.status, 'ARCHIVED')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('project API errors surface the backend message', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = (async () => jsonResponse({ message: 'Project name is required' }, 400)) as typeof fetch
  try {
    await assert.rejects(
      () => getProjects(),
      /Project name is required/,
    )
  } finally {
    globalThis.fetch = originalFetch
  }
})
