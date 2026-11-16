import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Task, TaskStatus } from './types.ts'

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

export async function updateTaskStatus(task: Task, status: TaskStatus): Promise<Task> {
  if (task.id == null) {
    throw new Error('Task has no id')
  }
  const response = await fetch(`${API_BASE_URL}/api/tasks/${task.id}`, {
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
  studentId: number,
  projectId?: number,
  signal?: AbortSignal,
): Promise<number> {
  const url = projectId != null
    ? `${API_BASE_URL}/api/students/${studentId}/progress?projectId=${projectId}`
    : `${API_BASE_URL}/api/students/${studentId}/progress`
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load student progress'))
  }
  return parseJsonResponse<number>(response, 'Could not load student progress')
}
