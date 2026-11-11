import assert from 'node:assert/strict'
import test from 'node:test'
import { assigneeLabel } from './users.ts'

test('assigneeLabel shows the student name without the id', () => {
  const users = [{ id: 7, name: 'Asha', email: null, role: 'STUDENT' as const }]

  assert.equal(assigneeLabel(7, users), 'Asha')
  assert.equal(assigneeLabel(7, users, 'Stored name'), 'Asha')
  assert.equal(assigneeLabel(null, [], 'Asha'), 'Asha')
  assert.equal(assigneeLabel(9, []), 'Unassigned')
})
