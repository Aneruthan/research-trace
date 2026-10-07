import { API_BASE_URL } from './config'
import type { Meeting } from './types'

export async function getProjectMeetings(
  projectId: number,
  signal?: AbortSignal,
): Promise<Meeting[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/meetings`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load meetings for project ${projectId} (${response.status})`)
  }
  return response.json() as Promise<Meeting[]>
}

export async function getMeeting(meetingId: number, signal?: AbortSignal): Promise<Meeting> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load meeting ${meetingId} (${response.status})`)
  }
  return response.json() as Promise<Meeting>
}
