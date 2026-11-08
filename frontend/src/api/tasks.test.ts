import test from 'node:test'
import assert from 'node:assert/strict'
import {
  updateTaskStatus,
  type Task,
} from './tasks.ts'

test('updateTaskStatus sends PUT to /api/tasks/:id with exact task id in URL and payload', async () => {
  const originalFetch = globalThis.fetch
  let capturedUrl = ''
  let capturedMethod = ''
  let capturedBody = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    capturedUrl = String(input)
    capturedMethod = init?.method ?? 'GET'
    capturedBody = String(init?.body ?? '')
    const updated: Task = {
      id: 42,
      projectId: 1,
      title: 'Task A',
      status: 'COMPLETED',
      milestoneId: 10,
    }
    return new Response(JSON.stringify(updated), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const taskA: Task = { id: 42, projectId: 1, title: 'Task A', status: 'IN_PROGRESS', milestoneId: 10 }
    const result = await updateTaskStatus(taskA, 'COMPLETED')

    assert.strictEqual(capturedMethod, 'PUT')
    assert(capturedUrl.endsWith('/api/tasks/42'))
    assert(!capturedUrl.includes('""'))
    assert.strictEqual(result.id, 42)
    assert.strictEqual(result.status, 'COMPLETED')

    const body = JSON.parse(capturedBody)
    assert.strictEqual(body.id, 42)
    assert.strictEqual(body.status, 'COMPLETED')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('Req 1 & 2: Updating task A in a list of [A, B, C] updates ONLY task A, leaving B and C untouched', async () => {
  const originalFetch = globalThis.fetch
  const calls: { url: string; id: number; status: string }[] = []

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    const body = JSON.parse(String(init?.body ?? '{}'))
    calls.push({ url, id: body.id, status: body.status })
    return new Response(JSON.stringify({ ...body, title: 'Task ' + body.id }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const taskList: Task[] = [
      { id: 10, projectId: 1, title: 'Task A', status: 'IN_PROGRESS' },
      { id: 20, projectId: 1, title: 'Task B', status: 'TODO' },
      { id: 30, projectId: 1, title: 'Task C', status: 'TODO' },
    ]

    // Simulate changeStatus handler in Tasks.tsx
    const taskToUpdate = taskList[0]
    const updated = await updateTaskStatus(taskToUpdate, 'COMPLETED')
    const updatedList = taskList.map((t) => (t.id === updated.id ? updated : t))

    assert.strictEqual(calls.length, 1)
    assert.strictEqual(calls[0].id, 10)
    assert(calls[0].url.endsWith('/api/tasks/10'))

    // Verify task A is updated
    assert.strictEqual(updatedList[0].id, 10)
    assert.strictEqual(updatedList[0].status, 'COMPLETED')

    // Verify task B and C are untouched
    assert.strictEqual(updatedList[1].id, 20)
    assert.strictEqual(updatedList[1].status, 'TODO')
    assert.strictEqual(updatedList[2].id, 30)
    assert.strictEqual(updatedList[2].status, 'TODO')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('Req 4: First item in task list can be updated without affecting other items', async () => {
  const originalFetch = globalThis.fetch
  let calledUrl = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    calledUrl = String(input)
    const body = JSON.parse(String(init?.body ?? '{}'))
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const tasks: Task[] = [
      { id: 2, projectId: 1, title: 'First Task', status: 'TODO' },
      { id: 3, projectId: 1, title: 'Second Task', status: 'TODO' },
      { id: 5, projectId: 1, title: 'Last Task', status: 'TODO' },
    ]

    // Update first task
    await updateTaskStatus(tasks[0], 'IN_PROGRESS')
    assert(calledUrl.endsWith('/api/tasks/2'), 'Must call /api/tasks/2 for first task')
    assert(!calledUrl.endsWith('/api/tasks/5'), 'Must NEVER call last task ID (5) when updating first task')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('Req 5: Last item in task list can be updated without affecting first item', async () => {
  const originalFetch = globalThis.fetch
  let calledUrl = ''

  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    calledUrl = String(input)
    const body = JSON.parse(String(init?.body ?? '{}'))
    return new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch

  try {
    const tasks: Task[] = [
      { id: 2, projectId: 1, title: 'First Task', status: 'TODO' },
      { id: 3, projectId: 1, title: 'Second Task', status: 'TODO' },
      { id: 5, projectId: 1, title: 'Last Task', status: 'TODO' },
    ]

    // Update last task
    await updateTaskStatus(tasks[tasks.length - 1], 'COMPLETED')
    assert(calledUrl.endsWith('/api/tasks/5'), 'Must call /api/tasks/5 for last task')
    assert(!calledUrl.endsWith('/api/tasks/2'), 'Must NEVER call first task ID (2) when updating last task')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('Req 6: List reordering preserves task identity by stable ID', () => {
  const tasks: Task[] = [
    { id: 2, projectId: 1, title: 'Beta Task', status: 'TODO' },
    { id: 3, projectId: 1, title: 'Alpha Task', status: 'IN_PROGRESS' },
    { id: 5, projectId: 1, title: 'Gamma Task', status: 'COMPLETED' },
  ]

  // Reorder tasks by title
  const reordered = [...tasks].sort((a, b) => a.title.localeCompare(b.title))

  // Find task with ID 2 in reordered array
  const task2 = reordered.find((t) => t.id === 2)
  assert.strictEqual(task2?.title, 'Beta Task')
  assert.strictEqual(task2?.status, 'TODO')

  // Find task with ID 5
  const task5 = reordered.find((t) => t.id === 5)
  assert.strictEqual(task5?.title, 'Gamma Task')
  assert.strictEqual(task5?.status, 'COMPLETED')
})

test('Req 14: Failed API update rejects and leaves previous state unchanged', async () => {
  const originalFetch = globalThis.fetch

  globalThis.fetch = (async () => {
    return new Response('Internal Server Error', {
      status: 500,
      headers: { 'Content-Type': 'text/plain' },
    })
  }) as typeof fetch

  try {
    let taskList: Task[] = [
      { id: 10, projectId: 1, title: 'Task 10', status: 'IN_PROGRESS' },
    ]

    let caughtError: unknown = null
    try {
      await updateTaskStatus(taskList[0], 'COMPLETED')
      taskList = [{ ...taskList[0], status: 'COMPLETED' }]
    } catch (err) {
      caughtError = err
      // In Tasks.tsx changeStatus, on error the state is NOT updated to COMPLETED;
      // instead load() is called to refresh from the server.
    }

    assert(caughtError !== null)
    // Verify state remained IN_PROGRESS
    assert.strictEqual(taskList[0].status, 'IN_PROGRESS')
  } finally {
    globalThis.fetch = originalFetch
  }
})
