import { API_BASE_URL, parseJsonResponse } from './config.ts'
import type { Milestone } from './types.ts'

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

export async function getMilestoneProgress(
  milestoneId: number,
  projectId?: number,
  signal?: AbortSignal,
): Promise<number> {
  const url = projectId != null
    ? `${API_BASE_URL}/api/milestones/${milestoneId}/progress?projectId=${projectId}`
    : `${API_BASE_URL}/api/milestones/${milestoneId}/progress`
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new Error(`Could not load milestone progress (${response.status})`)
  }
  return parseJsonResponse<number>(response, 'Could not load milestone progress')
}
