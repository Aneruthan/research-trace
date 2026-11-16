import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Milestone } from './types.ts'

export async function getProjectMilestones(
  projectId: number,
  signal?: AbortSignal,
): Promise<Milestone[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/milestones`, {
    signal,
    cache: 'no-store',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load milestones for project ${projectId}`))
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
  const response = await fetch(url, { signal, cache: 'no-store' })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load milestone progress'))
  }
  return parseJsonResponse<number>(response, 'Could not load milestone progress')
}

export async function updateMilestone(milestone: Milestone): Promise<Milestone> {
  if (milestone.id == null) {
    throw new Error('Milestone has no id')
  }
  const response = await fetch(`${API_BASE_URL}/api/milestones/${milestone.id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      projectId: milestone.projectId,
      title: milestone.title,
      description: milestone.description,
      status: milestone.status,
      dueDate: milestone.dueDate,
      weight: milestone.weight,
    }),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not update milestone ${milestone.id}`))
  }
  return parseJsonResponse<Milestone>(response, `Could not update milestone ${milestone.id}`)
}
