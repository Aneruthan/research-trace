import { API_BASE_URL, parseJsonResponse } from './config'
import type { User } from './types'

export async function getUsers(signal?: AbortSignal): Promise<User[]> {
  const response = await fetch(`${API_BASE_URL}/api/users`, { signal })
  if (!response.ok) {
    throw new Error(`Could not load users (${response.status})`)
  }
  return parseJsonResponse<User[]>(response, 'Could not load users')
}

export function assigneeLabel(
  assigneeId: number | null,
  users: User[],
  assigneeName?: string | null,
): string {
  const storedName = assigneeName?.trim() ? assigneeName.trim() : null
  const matchedName = assigneeId == null
    ? null
    : users.find((user) => user.id === assigneeId)?.name?.trim() || null
  const name = matchedName ?? storedName

  if (name != null && assigneeId != null) {
    return `${name} (${assigneeId})`
  }
  if (name != null) {
    return name
  }
  if (assigneeId != null) {
    return `Assignee ${assigneeId}`
  }
  return 'none'
}
