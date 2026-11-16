import type { ProjectMember, User } from '../api/types.ts'

const PREVIEW_COUNT = 3

export function selectedStudentIds(members: ProjectMember[]): number[] {
  return [...new Set(
    members
      .filter((member) => member.role === 'STUDENT' && member.userId != null)
      .map((member) => member.userId as number),
  )]
}

export function studentNames(members: ProjectMember[] | null | undefined, users: User[]): string[] {
  return selectedStudentIds(members ?? [])
    .map((userId) => users.find((user) => user.id === userId)?.name?.trim() || 'Unnamed student')
    .sort((left, right) => left.localeCompare(right))
}

export function formatAssignedStudents(names: string[], expanded = false): string {
  if (names.length === 0) {
    return 'Students: Unassigned'
  }
  if (expanded || names.length <= PREVIEW_COUNT) {
    return `Students: ${names.join(', ')}`
  }
  const preview = names.slice(0, PREVIEW_COUNT).join(', ')
  return `Students: ${preview} +${names.length - PREVIEW_COUNT} more`
}

export function hiddenStudentNames(names: string[]): string[] {
  return names.length <= PREVIEW_COUNT ? [] : names.slice(PREVIEW_COUNT)
}
