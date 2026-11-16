import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config'
import type { ProjectDashboard } from './types'

export async function getProjectDashboard(
  projectId: number,
  signal?: AbortSignal,
): Promise<ProjectDashboard> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/dashboard`, {
    signal,
    cache: 'no-store',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load project ${projectId}`))
  }
  return parseJsonResponse<ProjectDashboard>(
    response,
    `Could not load project ${projectId} dashboard`,
  )
}
