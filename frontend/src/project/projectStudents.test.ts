import assert from 'node:assert/strict'
import test from 'node:test'
import type { ProjectMember, User } from '../api/types.ts'
import {
  formatAssignedStudents,
  hiddenStudentNames,
  selectedStudentIds,
  studentNames,
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

test('existing project student names stay visible without changing memberships', () => {
  const current = [member(10, 2), member(11, 3), member(12, 9, 'PROFESSOR')]
  assert.deepEqual(selectedStudentIds(current), [2, 3])
  assert.deepEqual(studentNames(current, users), ['Alex Johnson', 'Priya Sharma'])
  assert.equal(formatAssignedStudents(studentNames(current, users)), 'Students: Alex Johnson, Priya Sharma')
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
