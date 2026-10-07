import { API_BASE_URL } from './config'
import type { ProjectDashboard } from './types'

export async function getProjectDashboard(
  projectId: number,
  signal?: AbortSignal,
): Promise<ProjectDashboard> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/dashboard`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load project ${projectId} (${response.status})`)
  }
  return response.json() as Promise<ProjectDashboard>
}
