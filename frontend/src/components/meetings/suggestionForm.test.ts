import assert from 'node:assert/strict'
import test from 'node:test'
import { assigneeChoices } from './suggestionForm.ts'

const students = [
  { id: 7, name: 'Asha' },
  { id: 8, name: '  ' },
]

test('assigneeChoices offers unassigned, student names, and the current unknown assignee', () => {
  assert.deepEqual(assigneeChoices(students, null), [
    { value: '', label: 'Unassigned' },
    { value: '7', label: 'Asha' },
    { value: '8', label: 'Unknown assignee' },
  ])
  assert.deepEqual(assigneeChoices(students, 9), [
    { value: '', label: 'Unassigned' },
    { value: '7', label: 'Asha' },
    { value: '8', label: 'Unknown assignee' },
    { value: '9', label: 'Unknown assignee' },
  ])
})

test('assigneeChoices keeps a known current assignee from being listed twice', () => {
  const choices = assigneeChoices(students, 7)
  assert.equal(choices.filter((choice) => choice.value === '7').length, 1)
})
