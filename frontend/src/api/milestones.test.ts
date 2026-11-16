import assert from 'node:assert/strict'
import test from 'node:test'
import { updateMilestone } from './milestones.ts'

test('updateMilestone sends the edited milestone to its own URL', async () => {
  const originalFetch = globalThis.fetch
  let capturedUrl = ''
  let capturedMethod = ''
  let capturedBody = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    capturedUrl = String(input)
    capturedMethod = init?.method ?? 'GET'
    capturedBody = String(init?.body ?? '')
    return new Response(JSON.stringify({
      id: 11,
      projectId: 1,
      title: 'Revised milestone',
      weight: 2.5,
      status: 'IN_PROGRESS',
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const saved = await updateMilestone({
      id: 11,
      projectId: 1,
      title: 'Revised milestone',
      description: 'Updated scope',
      status: 'IN_PROGRESS',
      dueDate: '2026-11-20',
      weight: 2.5,
    })

    assert.equal(capturedMethod, 'PUT')
    assert.ok(capturedUrl.endsWith('/api/projects/1/milestones/11'))
    assert.match(capturedBody, /"title":"Revised milestone"/)
    assert.match(capturedBody, /"weight":2.5/)
    assert.equal(saved.id, 11)
    assert.equal(saved.title, 'Revised milestone')
  } finally {
    globalThis.fetch = originalFetch
  }
})
