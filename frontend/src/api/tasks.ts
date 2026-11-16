import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Task, TaskStatus } from './types.ts'

export async function getProjectTask(
  projectId: number,
  taskId: number,
  signal?: AbortSignal,
): Promise<Task> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks/${taskId}`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load task ${taskId}`))
  }
  return parseJsonResponse<Task>(response, `Could not load task ${taskId}`)
}

export async function getProjectTasks(projectId: number, signal?: AbortSignal): Promise<Task[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks`, {
    signal,
    cache: 'no-store',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load tasks for project ${projectId}`))
  }
  return parseJsonResponse<Task[]>(response, `Could not load tasks for project ${projectId}`)
}

export async function createProjectTask(
  projectId: number,
  task: Pick<Task, 'title' | 'description' | 'assigneeId' | 'milestoneId' | 'dueDate' | 'status'>,
): Promise<Task> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(task),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not create task'))
  }
  return parseJsonResponse<Task>(response, 'Could not create task')
}

export async function deleteProjectTask(projectId: number, taskId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks/${taskId}`, { method: 'DELETE' })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not delete task ${taskId}`))
  }
}

export async function updateProjectTask(task: Task): Promise<Task> {
  if (task.id == null || task.projectId == null) {
    throw new Error('Task is missing its project')
  }
  const response = await fetch(`${API_BASE_URL}/api/projects/${task.projectId}/tasks/${task.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: task.title,
      description: task.description,
      assigneeId: task.assigneeId,
      milestoneId: task.milestoneId,
      dueDate: task.dueDate,
      status: task.status,
      meetingId: task.meetingId,
    }),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not update task ${task.id}`))
  }
  return parseJsonResponse<Task>(response, `Could not update task ${task.id}`)
}

export async function getTaskHistory(projectId: number, taskId: number, signal?: AbortSignal) {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks/${taskId}/history`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load task history'))
  }
  return parseJsonResponse<Array<{
    id: number | null
    previousStatus: string | null
    newStatus: string | null
    changedAt: string | null
  }>>(response, 'Could not load task history')
}

export async function updateTaskStatus(task: Task, status: TaskStatus): Promise<Task> {
  if (task.id == null) {
    throw new Error('Task has no id')
  }
  if (task.projectId == null) {
    throw new Error(`Task ${task.id} has no project`)
  }
  const response = await fetch(`${API_BASE_URL}/api/projects/${task.projectId}/tasks/${task.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      id: task.id,
      title: task.title,
      description: task.description,
      assigneeId: task.assigneeId,
      milestoneId: task.milestoneId,
      dueDate: task.dueDate,
      status,
    }),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not update task ${task.id}`))
  }
  return parseJsonResponse<Task>(response, `Could not update task ${task.id}`)
}

export async function getProjectProgress(projectId: number, signal?: AbortSignal): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/progress`, {
    signal,
    cache: 'no-store',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load project progress'))
  }
  return parseJsonResponse<number>(response, 'Could not load project progress')
}

export async function getStudentProgress(
  projectId: number,
  studentId: number,
  signal?: AbortSignal,
): Promise<number> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/students/${studentId}/progress`,
    { signal },
  )
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load student progress'))
  }
  return parseJsonResponse<number>(response, 'Could not load student progress')
}
