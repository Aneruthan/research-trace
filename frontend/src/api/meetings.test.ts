import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createProjectMeeting,
  updateMeeting,
  getProjectMeetings,
  getMeeting,
} from './meetings.ts'

test('createProjectMeeting calls POST on project meetings endpoint and parses response', async () => {
  const originalFetch = globalThis.fetch
  let capturedUrl = ''
  let capturedMethod = ''
  let capturedBody = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    capturedUrl = String(input)
    capturedMethod = init?.method ?? 'GET'
    capturedBody = String(init?.body ?? '')
    const mockMeeting = {
      id: 42,
      projectId: 1,
      title: 'New Supervision Meeting',
      meetingDate: '2026-11-08',
      notes: 'Supervision transcript',
    }
    return new Response(JSON.stringify(mockMeeting), {
      status: 201,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const result = await createProjectMeeting(1, {
      title: 'New Supervision Meeting',
      meetingDate: '2026-11-08',
      notes: 'Supervision transcript',
    })

    assert.strictEqual(capturedMethod, 'POST')
    assert(capturedUrl.includes('/api/projects/1/meetings'))
    assert(!capturedUrl.includes('""'))
    assert.strictEqual(result.id, 42)
    assert.strictEqual(result.title, 'New Supervision Meeting')
    const parsedBody = JSON.parse(capturedBody)
    assert.strictEqual(parsedBody.title, 'New Supervision Meeting')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('updateMeeting calls PUT on meeting endpoint and merges updates', async () => {
  const originalFetch = globalThis.fetch
  let capturedUrl = ''
  let capturedMethod = ''
  let capturedBody = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    capturedUrl = String(input)
    capturedMethod = init?.method ?? 'GET'
    capturedBody = String(init?.body ?? '')
    const updatedMeeting = {
      id: 42,
      projectId: 1,
      title: 'Updated Meeting Title',
      meetingDate: '2026-11-09',
      notes: 'Expanded notes',
    }
    return new Response(JSON.stringify(updatedMeeting), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const result = await updateMeeting(42, {
      title: 'Updated Meeting Title',
      meetingDate: '2026-11-09',
      notes: 'Expanded notes',
    })

    assert.strictEqual(capturedMethod, 'PUT')
    assert(capturedUrl.includes('/api/meetings/42'))
    assert(!capturedUrl.includes('""'))
    assert.strictEqual(result.id, 42)
    assert.strictEqual(result.title, 'Updated Meeting Title')
    assert(capturedBody.includes('Updated Meeting Title'))
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('getProjectMeetings and getMeeting fetch correctly', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = (async (input: RequestInfo | URL) => {
    const url = String(input)
    if (url.includes('/api/projects/1/meetings')) {
      return new Response(JSON.stringify([{ id: 1, title: 'Meeting 1' }]), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }
    if (url.includes('/api/meetings/1')) {
      return new Response(JSON.stringify({ id: 1, title: 'Meeting 1' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    }
    return new Response('Not found', { status: 404 })
  }) as typeof fetch

  try {
    const list = await getProjectMeetings(1)
    assert.strictEqual(list.length, 1)
    assert.strictEqual(list[0].id, 1)

    const single = await getMeeting(1)
    assert.strictEqual(single.id, 1)
  } finally {
    globalThis.fetch = originalFetch
  }
})
