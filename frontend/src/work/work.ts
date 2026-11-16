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

export type StudentFilterOption = {
  userId: number
  label: string
  member: boolean
}

export function studentFilterOptions(
  members: { userId: number | null; role: string | null }[],
  tasks: { assigneeId?: number | null }[],
  nameFor: (userId: number) => string,
): StudentFilterOption[] {
  const memberIds = new Set(
    members
      .filter((member) => member.role === 'STUDENT' && member.userId != null)
      .map((member) => member.userId as number),
  )
  const ids = new Set<number>(memberIds)
  for (const task of tasks) {
    if (task.assigneeId != null) {
      ids.add(task.assigneeId)
    }
  }
  return [...ids]
    .map((userId) => {
      const name = nameFor(userId)
      return {
        userId,
        member: memberIds.has(userId),
        label: memberIds.has(userId) ? name : `${name} (not a project member)`,
      }
    })
    .sort((left, right) => left.label.localeCompare(right.label))
}

export function validateTaskDraft(title: string): string | null {
  if (title.trim().length === 0) return 'Task title is required.'
  return null
}

export type StudentTaskDraft = {
  title: string
  description: string
  dueDate: string
  status: TaskStatus
  milestoneId: number | null
  assigneeId: number
}

export function studentTaskDraft(studentId: number): StudentTaskDraft {
  return {
    title: '',
    description: '',
    dueDate: '',
    status: 'TODO',
    milestoneId: null,
    assigneeId: studentId,
  }
}

export function validateStudentTask(
  draft: StudentTaskDraft,
  options: { studentIsMember: boolean; milestoneIds: number[] },
): string | null {
  if (draft.title.trim().length === 0) {
    return 'Task title is required.'
  }
  if (!options.studentIsMember || !Number.isInteger(draft.assigneeId)) {
    return 'Student must belong to the active project.'
  }
  if (draft.milestoneId != null && !options.milestoneIds.includes(draft.milestoneId)) {
    return 'Milestone must belong to the active project.'
  }
  return null
}

export function studentTaskEmptyMessage(taskCount: number): string | null {
  return taskCount === 0 ? 'No tasks assigned yet.' : null
}

export function validateMilestoneDraft(title: string, weight: string): string | null {
  if (title.trim().length === 0) return 'Milestone title is required.'
  if (weight.trim().length > 0 && Number(weight) < 0) return 'Milestone weight cannot be negative.'
  return null
}
