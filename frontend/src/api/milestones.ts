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
  projectId: number,
  milestoneId: number,
  signal?: AbortSignal,
): Promise<number> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/milestones/${milestoneId}/progress`,
    { signal, cache: 'no-store' },
  )
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load milestone progress'))
  }
  return parseJsonResponse<number>(response, 'Could not load milestone progress')
}

export async function createMilestone(
  projectId: number,
  milestone: Pick<Milestone, 'title' | 'description' | 'status' | 'dueDate' | 'weight'>,
): Promise<Milestone> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/milestones`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(milestone),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not create milestone'))
  }
  return parseJsonResponse<Milestone>(response, 'Could not create milestone')
}

export async function updateMilestone(milestone: Milestone): Promise<Milestone> {
  if (milestone.id == null) {
    throw new Error('Milestone has no id')
  }
  if (milestone.projectId == null) {
    throw new Error(`Milestone ${milestone.id} has no project`)
  }
  const response = await fetch(`${API_BASE_URL}/api/projects/${milestone.projectId}/milestones/${milestone.id}`, {
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
