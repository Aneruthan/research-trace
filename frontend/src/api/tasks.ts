import { API_BASE_URL, parseJsonResponse } from './config'
import type { Task, TaskStatus } from './types'

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
  return response.json() as Promise<Task>
}

export async function getProjectProgress(projectId: number, signal?: AbortSignal): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/progress`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load project progress (${response.status})`)
  }
  return response.json() as Promise<number>
}
