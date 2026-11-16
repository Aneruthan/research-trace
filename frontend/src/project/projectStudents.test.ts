import assert from 'node:assert/strict'
import test from 'node:test'
import type { ProjectMember, User } from '../api/types.ts'
import {
  formatAssignedStudents,
  hiddenStudentNames,
  membershipChanges,
  selectedStudentIds,
  studentNames,
  studentUsers,
  validateStudentSelection,
} from './projectStudents.ts'

const users: User[] = [
  { id: 2, name: 'Alex Johnson', email: 'alex@research.edu', role: 'STUDENT' },
  { id: 3, name: 'Priya Sharma', email: 'priya@research.edu', role: 'STUDENT' },
  { id: 4, name: 'Rahul Kumar', email: 'rahul@research.edu', role: 'STUDENT' },
  { id: 9, name: 'Prof. Verma', email: 'verma@research.edu', role: 'PROFESSOR' },
]

function member(id: number, userId: number, role: ProjectMember['role'] = 'STUDENT'): ProjectMember {
  return { id, projectId: 1, userId, role }
}

test('project creation can select multiple existing students and reject other users', () => {
  assert.deepEqual(studentUsers(users).map((user) => user.name), ['Alex Johnson', 'Priya Sharma', 'Rahul Kumar'])
  assert.equal(validateStudentSelection([2, 3, 4], users), null)
  assert.equal(validateStudentSelection([], users), null)
  assert.match(validateStudentSelection([9], users) ?? '', /existing students/)
  const plan = membershipChanges([], [2, 3, 3, 4])
  assert.deepEqual(plan.addUserIds, [2, 3, 4])
  assert.deepEqual(plan.removeMembershipIds, [])
})

test('editing a project adds a student without duplicating current memberships', () => {
  const current = [member(10, 2), member(11, 3)]
  const plan = membershipChanges(current, [2, 3, 4])
  assert.deepEqual(plan.addUserIds, [4])
  assert.deepEqual(plan.removeMembershipIds, [])
  assert.deepEqual(selectedStudentIds(current), [2, 3])
})

test('removing a student drops the membership and keeps the user id out of the delete list', () => {
  const current = [member(10, 2), member(11, 3), member(12, 9, 'PROFESSOR')]
  const plan = membershipChanges(current, [2])
  assert.deepEqual(plan.removeMembershipIds, [11])
  assert.equal(plan.removeMembershipIds.includes(3), false)
  assert.deepEqual(plan.addUserIds, [])
})

test('unassigned and multiple student names stay readable', () => {
  assert.equal(formatAssignedStudents([]), 'Students: Unassigned')
  const names = studentNames([member(10, 4), member(11, 2), member(12, 3)], users)
  assert.deepEqual(names, ['Alex Johnson', 'Priya Sharma', 'Rahul Kumar'])
  assert.equal(formatAssignedStudents(names), 'Students: Alex Johnson, Priya Sharma, Rahul Kumar')
  const many = [...names, 'Asha Rao', 'Dev Patel']
  assert.match(formatAssignedStudents(many), /Alex Johnson, Priya Sharma, Rahul Kumar \+2 more/)
  assert.equal(formatAssignedStudents(many, true), `Students: ${many.join(', ')}`)
  assert.deepEqual(hiddenStudentNames(many), ['Asha Rao', 'Dev Patel'])
  assert.deepEqual(hiddenStudentNames(names), [])
})
