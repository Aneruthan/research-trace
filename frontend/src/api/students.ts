import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Task } from './types.ts'

export type StudentWork = {
  userId: number | null
  membershipId: number | null
  projectId: number | null
  name: string | null
  email: string | null
  role: string | null
  totalTasks: number
  completedTasks: number
  inProgressTasks: number
  pendingTasks: number
  overdueTasks: number
  progress: number | null
  projectMembershipCount: number
  tasks: Task[] | null
}

export async function getProjectStudents(projectId: number, signal?: AbortSignal): Promise<StudentWork[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/students`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load students'))
  }
  return parseJsonResponse<StudentWork[]>(response, 'Could not load students')
}

export async function getProjectStudent(
  projectId: number,
  studentId: number,
  signal?: AbortSignal,
): Promise<StudentWork> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/students/${studentId}`, { signal })
  if (!response.ok) {
    throw new Error(await readErrorMessage(response, 'Could not load student'))
  }
  return parseJsonResponse<StudentWork>(response, 'Could not load student')
}
