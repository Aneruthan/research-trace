import { API_BASE_URL, parseJsonResponse } from './config.ts'
import type { Task, TaskStatus } from './types.ts'

export async function getProjectTasks(projectId: number, signal?: AbortSignal): Promise<Task[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/tasks`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load tasks for project ${projectId} (${response.status})`)
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
    throw new Error(`Could not update task ${task.id} (${response.status})`)
  }
  return parseJsonResponse<Task>(response, `Could not update task ${task.id}`)
}

export async function getProjectProgress(projectId: number, signal?: AbortSignal): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/progress`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load project progress (${response.status})`)
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
    throw new Error(`Could not load student progress (${response.status})`)
  }
  return parseJsonResponse<number>(response, 'Could not load student progress')
}
