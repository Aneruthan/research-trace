import { API_BASE_URL } from './config'
import type { SuggestionUpdate, TaskSuggestion } from './types'

export async function getTaskSuggestions(
  meetingId: number,
  signal?: AbortSignal,
): Promise<TaskSuggestion[]> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/task-suggestions`, {
    signal,
  })
  if (!response.ok) {
    throw new Error(`Could not load task suggestions (${response.status})`)
  }
  return response.json() as Promise<TaskSuggestion[]>
}

export async function extractTaskSuggestions(meetingId: number): Promise<TaskSuggestion[]> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/task-suggestions/extract`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(`Could not extract tasks (${response.status})`)
  }
  return response.json() as Promise<TaskSuggestion[]>
}

export async function updateTaskSuggestion(
  suggestionId: number,
  update: SuggestionUpdate,
): Promise<TaskSuggestion> {
  const response = await fetch(`${API_BASE_URL}/api/task-suggestions/${suggestionId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(update),
  })
  if (!response.ok) {
    throw new Error(`Could not update suggestion (${response.status})`)
  }
  return response.json() as Promise<TaskSuggestion>
}

export async function acceptTaskSuggestion(suggestionId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/task-suggestions/${suggestionId}/accept`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(`Could not accept suggestion (${response.status})`)
  }
}

export async function rejectTaskSuggestion(suggestionId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/task-suggestions/${suggestionId}/reject`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(`Could not reject suggestion (${response.status})`)
  }
}
