import { API_BASE_URL, parseJsonResponse } from './config'
import type { Milestone } from './types'

export async function getProjectMilestones(
  projectId: number,
  signal?: AbortSignal,
): Promise<Milestone[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/milestones`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load milestones for project ${projectId} (${response.status})`)
  }
  return parseJsonResponse<Milestone[]>(response, `Could not load milestones for project ${projectId}`)
}

export async function getMilestoneProgress(milestoneId: number, signal?: AbortSignal): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/api/milestones/${milestoneId}/progress`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load milestone progress (${response.status})`)
  }
  return response.json() as Promise<number>
}
