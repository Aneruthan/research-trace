import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { ProjectMember, User } from './types'

export async function createUser(user: Pick<User, 'name' | 'email' | 'role'>): Promise<User> {
  const response = await fetch(`${API_BASE_URL}/api/users`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not create student'))
  }
  return parseJsonResponse<User>(response, 'Could not create student')
}

export async function updateUser(userId: number, user: Pick<User, 'name' | 'email' | 'role'>): Promise<User> {
  const response = await fetch(`${API_BASE_URL}/api/users/${userId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not update student'))
  }
  return parseJsonResponse<User>(response, 'Could not update student')
}

export async function getUsers(signal?: AbortSignal): Promise<User[]> {
  const response = await fetch(`${API_BASE_URL}/api/users`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load users'))
  }
  return parseJsonResponse<User[]>(response, 'Could not load users')
}

export async function getProjectMembers(
  projectId: number,
  signal?: AbortSignal,
): Promise<ProjectMember[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/members`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, `Could not load members for project ${projectId}`))
  }
  return parseJsonResponse<ProjectMember[]>(response, `Could not load members for project ${projectId}`)
}

export async function addProjectMember(
  projectId: number,
  member: Pick<ProjectMember, 'userId' | 'role'>,
): Promise<ProjectMember> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/members`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(member),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not add project member'))
  }
  return parseJsonResponse<ProjectMember>(response, 'Could not add project member')
}

export async function updateProjectMember(
  projectId: number,
  memberId: number,
  member: Partial<Pick<ProjectMember, 'userId' | 'role'>>,
): Promise<ProjectMember> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/members/${memberId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(member),
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not update project member'))
  }
  return parseJsonResponse<ProjectMember>(response, 'Could not update project member')
}

export async function removeProjectMember(projectId: number, memberId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/members/${memberId}`, {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not remove project member'))
  }
}

export function assigneeLabel(
  assigneeId: number | null,
  users: User[],
  assigneeName?: string | null,
): string {
  const storedName = assigneeName?.trim() ? assigneeName.trim() : null
  if (assigneeId == null) {
    return storedName ?? 'Unassigned'
  }
  return userName(assigneeId, users) ?? storedName ?? 'Unknown assignee'
}

export function studentLabel(studentId: number | null, users: User[]): string {
  return (studentId == null ? null : userName(studentId, users)) ?? 'Unknown student'
}

function userName(userId: number, users: User[]): string | null {
  return users.find((user) => user.id === userId)?.name?.trim() || null
}
