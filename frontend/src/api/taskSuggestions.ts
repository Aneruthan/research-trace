import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { SuggestionUpdate, TaskSuggestion } from './types.ts'

export async function getTaskSuggestions(
  meetingId: number,
  signal?: AbortSignal,
): Promise<TaskSuggestion[]> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/task-suggestions`, {
    signal,
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load task suggestions'))
  }
  return parseJsonResponse<TaskSuggestion[]>(response)
}

export async function extractTaskSuggestions(meetingId: number): Promise<TaskSuggestion[]> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/task-suggestions/extract`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not extract tasks'))
  }
  return parseJsonResponse<TaskSuggestion[]>(response)
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
    throw new Error(await readErrorMessage(response, 'Could not update suggestion'))
  }
  return parseJsonResponse<TaskSuggestion>(response)
}

export async function acceptTaskSuggestion(suggestionId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/task-suggestions/${suggestionId}/accept`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not accept suggestion'))
  }
}

export async function rejectTaskSuggestion(suggestionId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/task-suggestions/${suggestionId}/reject`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not reject suggestion'))
  }
}
