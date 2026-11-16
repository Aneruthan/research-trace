import assert from 'node:assert/strict'
import test from 'node:test'
import type { Task } from '../api/types.ts'
import {
  filterTasks,
  isOverdueTask,
  taskCounts,
  validateMilestoneDraft,
  validatePerson,
  validateTaskDraft,
} from './work.ts'

const today = '2026-11-17'

test('student list counts overdue work separately from pending work', () => {
  const tasks: Task[] = [
    task(1, 'COMPLETED', '2026-11-01', 7),
    task(2, 'IN_PROGRESS', '2026-11-10', 7),
    task(3, 'TODO', '2026-11-20', 7),
    task(4, 'TODO', '2026-11-16', 8),
  ]
  const counts = taskCounts(tasks.filter((item) => item.assigneeId === 7), today)
  assert.equal(counts.total, 3)
  assert.equal(counts.completed, 1)
  assert.equal(counts.inProgress, 1)
  assert.equal(counts.pending, 1)
  assert.equal(counts.overdue, 1)
  assert.equal(isOverdueTask(tasks[0], today), false)
  assert.equal(taskCounts([], today).total, 0)
})

test('task filters stay inside the supplied project task list', () => {
  const tasks: Task[] = [
    task(1, 'TODO', '2026-11-01', null, null),
    task(2, 'IN_PROGRESS', '2026-12-01', 7, 3),
    task(3, 'COMPLETED', null, 7, 3),
  ]
  assert.equal(filterTasks(tasks, 'OVERDUE', today, null, null).length, 1)
  assert.equal(filterTasks(tasks, 'UNASSIGNED', today, null, null)[0].id, 1)
  assert.equal(filterTasks(tasks, 'COMPLETED', today, 7, 3)[0].id, 3)
  assert.equal(filterTasks(tasks, 'ALL', today, null, 'none')[0].id, 1)
  assert.equal(filterTasks(tasks, 'ALL', today, 7, 3).length, 2)
})

test('create forms require a name, email, and title', () => {
  assert.equal(validatePerson('', 'a@b.com'), 'Name is required.')
  assert.equal(validatePerson('Alex', 'alex'), 'Email must include @.')
  assert.equal(validatePerson('Alex', 'alex@example.com'), null)
  assert.equal(validateTaskDraft('  '), 'Task title is required.')
  assert.equal(validateTaskDraft('Write notes'), null)
  assert.equal(validateMilestoneDraft('', ''), 'Milestone title is required.')
  assert.equal(validateMilestoneDraft('Plan', '-1'), 'Milestone weight cannot be negative.')
  assert.equal(validateMilestoneDraft('Plan', ''), null)
})

function task(
  id: number,
  status: Task['status'],
  dueDate: string | null,
  assigneeId: number | null = null,
  milestoneId: number | null = null,
): Task {
  return { id, projectId: 1, title: `Task ${id}`, status, dueDate, assigneeId, milestoneId }
}
