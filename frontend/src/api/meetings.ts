import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Meeting } from './types.ts'

export interface CreateMeetingRequest {
  title: string
  meetingDate: string
  notes?: string
}

export interface UpdateMeetingRequest {
  title?: string
  meetingDate?: string
  notes?: string
}

export async function getProjectMeetings(
  projectId: number,
  signal?: AbortSignal,
): Promise<Meeting[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/meetings`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load meetings for project ${projectId}`))
  }
  return parseJsonResponse<Meeting[]>(response, `Could not load meetings for project ${projectId}`)
}

export async function getMeeting(meetingId: number, signal?: AbortSignal): Promise<Meeting> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load meeting ${meetingId}`))
  }
  return parseJsonResponse<Meeting>(response, `Could not load meeting ${meetingId}`)
}

export async function createProjectMeeting(
  projectId: number,
  request: CreateMeetingRequest,
): Promise<Meeting> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/meetings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not create meeting'))
  }
  return parseJsonResponse<Meeting>(response, 'Could not create meeting')
}

export async function updateMeeting(
  meetingId: number,
  request: UpdateMeetingRequest,
): Promise<Meeting> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not update meeting ${meetingId}`))
  }
  return parseJsonResponse<Meeting>(response, `Could not update meeting ${meetingId}`)
}

export async function deleteMeeting(meetingId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not delete meeting ${meetingId}`))
  }
}
