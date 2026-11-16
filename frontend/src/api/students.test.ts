import assert from 'node:assert/strict'
import test from 'node:test'
import { createMilestone } from './milestones.ts'
import { getProjectStudent, getProjectStudents } from './students.ts'
import { createProjectTask } from './tasks.ts'
import { addProjectMember, createUser, removeProjectMember, updateUser } from './users.ts'

test('student and work requests use the active project', async () => {
  const originalFetch = globalThis.fetch
  const calls: string[] = []
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    calls.push(`${init?.method ?? 'GET'} ${String(input)}`)
    const path = String(input)
    if (path.endsWith('/students')) {
      return json([{ userId: 7, name: 'Alex Johnson', projectMembershipCount: 2, progress: 50 }])
    }
    if (path.endsWith('/students/7')) {
      return json({ userId: 7, name: 'Alex Johnson', tasks: [], progress: 0 })
    }
    if (path.endsWith('/api/users') && init?.method === 'POST') {
      return json({ id: 9, name: 'New Student', email: 'new@example.com', role: 'STUDENT' }, 201)
    }
    if (path.endsWith('/api/users/9') && init?.method === 'PUT') {
      return json({ id: 9, name: 'Edited Student', email: 'new@example.com', role: 'STUDENT' })
    }
    if (path.endsWith('/members') && init?.method === 'POST') {
      return json({ id: 3, projectId: 2, userId: 9, role: 'STUDENT' }, 201)
    }
    if (path.endsWith('/members/3') && init?.method === 'DELETE') {
      return new Response(null, { status: 204 })
    }
    if (path.endsWith('/milestones') && init?.method === 'POST') {
      return json({ id: 4, projectId: 2, title: 'Plan' }, 201)
    }
    if (path.endsWith('/tasks') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body))
      assert.equal(body.projectId, undefined)
      return json({ id: 8, projectId: 2, title: body.title, assigneeId: body.assigneeId }, 201)
    }
    return new Response('Not found', { status: 404 })
  }) as typeof fetch

  try {
    const students = await getProjectStudents(2)
    assert.equal(students[0].projectMembershipCount, 2)
    const detail = await getProjectStudent(2, 7)
    assert.equal(detail.userId, 7)
    const created = await createUser({ name: 'New Student', email: 'new@example.com', role: 'STUDENT' })
    assert.equal(created.id, 9)
    const edited = await updateUser(9, { name: 'Edited Student', email: 'new@example.com', role: 'STUDENT' })
    assert.equal(edited.name, 'Edited Student')
    await addProjectMember(2, { userId: 9, role: 'STUDENT' })
    await removeProjectMember(2, 3)
    await createMilestone(2, { title: 'Plan', description: null, status: 'NOT_STARTED', dueDate: null, weight: null })
    const task = await createProjectTask(2, {
      title: 'Write notes',
      description: null,
      assigneeId: 9,
      milestoneId: 4,
      dueDate: '2026-12-01',
      status: 'TODO',
    })
    assert.equal(task.projectId, 2)
    assert.ok(calls.some((call) => call.includes('GET ') && call.includes('/api/projects/2/students')))
    assert.ok(calls.some((call) => call.includes('POST ') && call.includes('/api/projects/2/tasks')))
  } finally {
    globalThis.fetch = originalFetch
  }
})

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
