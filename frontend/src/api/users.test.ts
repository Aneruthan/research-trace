import assert from 'node:assert/strict'
import test from 'node:test'
import { assigneeLabel, studentLabel } from './users.ts'

const users = [{ id: 7, name: 'Asha', email: null, role: 'STUDENT' as const }]

test('assigneeLabel shows the student name without the id', () => {
  assert.equal(assigneeLabel(7, users), 'Asha')
  assert.equal(assigneeLabel(7, users, 'Stored name'), 'Asha')
  assert.equal(assigneeLabel(null, [], 'Asha'), 'Asha')
})

test('assigneeLabel distinguishes an unresolved assignee from an unassigned task', () => {
  assert.equal(assigneeLabel(null, users), 'Unassigned')
  assert.equal(assigneeLabel(9, []), 'Unknown assignee')
  assert.equal(assigneeLabel(9, users), 'Unknown assignee')
  assert.equal(assigneeLabel(9, [], 'Stored name'), 'Stored name')
})

test('studentLabel never labels a student as unassigned', () => {
  assert.equal(studentLabel(7, users), 'Asha')
  assert.equal(studentLabel(9, users), 'Unknown student')
  assert.equal(studentLabel(null, users), 'Unknown student')
})
