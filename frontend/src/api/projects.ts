import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Project, ProjectWriteRequest } from './types.ts'

export async function getProjects(signal?: AbortSignal): Promise<Project[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load projects'))
  }
  return parseJsonResponse<Project[]>(response, 'Could not load projects')
}

export async function createProject(request: ProjectWriteRequest): Promise<Project> {
  const response = await fetch(`${API_BASE_URL}/api/projects`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not create project'))
  }
  return parseJsonResponse<Project>(response, 'Could not create project')
}

export async function updateProject(projectId: number, request: ProjectWriteRequest): Promise<Project> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not update project'))
  }
  return parseJsonResponse<Project>(response, 'Could not update project')
}

export async function archiveProject(projectId: number): Promise<Project> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/archive`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not archive project'))
  }
  return parseJsonResponse<Project>(response, 'Could not archive project')
}
