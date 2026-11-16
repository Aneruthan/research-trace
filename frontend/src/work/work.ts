import type { Task, TaskStatus } from '../api/types.ts'

export type TaskListFilter = 'ALL' | TaskStatus | 'OVERDUE' | 'UNASSIGNED'

export function isOverdueTask(
  task: { dueDate?: string | null; status?: string | null },
  today: string,
): boolean {
  return Boolean(task.dueDate) && (task.dueDate as string) < today && task.status !== 'COMPLETED'
}

export function taskCounts(tasks: Task[], today: string) {
  const unique = new Map<number, Task>()
  for (const task of tasks) {
    if (task.id != null) {
      unique.set(task.id, task)
    }
  }
  const values = [...unique.values()]
  return {
    total: values.length,
    completed: values.filter((task) => task.status === 'COMPLETED').length,
    inProgress: values.filter((task) => task.status === 'IN_PROGRESS').length,
    pending: values.filter((task) => task.status !== 'COMPLETED' && task.status !== 'IN_PROGRESS').length,
    overdue: values.filter((task) => isOverdueTask(task, today)).length,
  }
}

export function filterTasks(
  tasks: Task[],
  filter: TaskListFilter,
  today: string,
  studentId: number | null,
  milestoneId: number | 'none' | null,
): Task[] {
  return tasks.filter((task) => {
    if (filter === 'OVERDUE' && !isOverdueTask(task, today)) return false
    if (filter === 'UNASSIGNED' && task.assigneeId != null) return false
    if (filter === 'TODO' || filter === 'IN_PROGRESS' || filter === 'COMPLETED') {
      if (task.status !== filter) return false
    }
    if (studentId != null && task.assigneeId !== studentId) return false
    if (milestoneId === 'none' && task.milestoneId != null) return false
    if (typeof milestoneId === 'number' && task.milestoneId !== milestoneId) return false
    return true
  })
}

export function validatePerson(name: string, email: string): string | null {
  if (name.trim().length === 0) return 'Name is required.'
  if (email.trim().length === 0) return 'Email is required.'
  if (!email.includes('@')) return 'Email must include @.'
  return null
}

export function validateTaskDraft(title: string): string | null {
  if (title.trim().length === 0) return 'Task title is required.'
  return null
}

export function validateMilestoneDraft(title: string, weight: string): string | null {
  if (title.trim().length === 0) return 'Milestone title is required.'
  if (weight.trim().length > 0 && Number(weight) < 0) return 'Milestone weight cannot be negative.'
  return null
}
