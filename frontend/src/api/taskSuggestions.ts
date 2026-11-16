import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { SuggestionUpdate, TaskSuggestion } from './types.ts'

export async function getTaskSuggestions(
  projectId: number,
  meetingId: number,
  signal?: AbortSignal,
): Promise<TaskSuggestion[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/meetings/${meetingId}/task-suggestions`,
    {
    signal,
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load task suggestions'))
  }
  return parseJsonResponse<TaskSuggestion[]>(response)
}

export async function extractTaskSuggestions(projectId: number, meetingId: number): Promise<TaskSuggestion[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/meetings/${meetingId}/task-suggestions/extract`,
    {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not extract tasks'))
  }
  return parseJsonResponse<TaskSuggestion[]>(response)
}

export async function updateTaskSuggestion(
  projectId: number,
  meetingId: number,
  suggestionId: number,
  update: SuggestionUpdate,
): Promise<TaskSuggestion> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/meetings/${meetingId}/task-suggestions/${suggestionId}`,
    {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(update),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not update suggestion'))
  }
  return parseJsonResponse<TaskSuggestion>(response)
}

export async function acceptTaskSuggestion(
  projectId: number,
  meetingId: number,
  suggestionId: number,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/meetings/${meetingId}/task-suggestions/${suggestionId}/accept`,
    {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not accept suggestion'))
  }
}

export async function rejectTaskSuggestion(
  projectId: number,
  meetingId: number,
  suggestionId: number,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/meetings/${meetingId}/task-suggestions/${suggestionId}/reject`,
    {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not reject suggestion'))
  }
}
