import type { ProjectMember, User } from '../api/types.ts'

const PREVIEW_COUNT = 3

export function studentUsers(users: User[]): User[] {
  return users
    .filter((user) => user.role === 'STUDENT' && user.id != null)
    .sort((left, right) => (left.name ?? '').localeCompare(right.name ?? ''))
}

export function selectedStudentIds(members: ProjectMember[]): number[] {
  return [...new Set(
    members
      .filter((member) => member.role === 'STUDENT' && member.userId != null)
      .map((member) => member.userId as number),
  )]
}

export function membershipChanges(
  current: ProjectMember[],
  selectedIds: number[],
): { addUserIds: number[]; removeMembershipIds: number[] } {
  const wanted = [...new Set(selectedIds)]
  const studentMembers = current.filter(
    (member) => member.role === 'STUDENT' && member.userId != null && member.id != null,
  )
  const currentIds = new Set(studentMembers.map((member) => member.userId as number))
  const wantedIds = new Set(wanted)
  return {
    addUserIds: wanted.filter((userId) => !currentIds.has(userId)),
    removeMembershipIds: studentMembers
      .filter((member) => !wantedIds.has(member.userId as number))
      .map((member) => member.id as number),
  }
}

export function validateStudentSelection(selectedIds: number[], users: User[]): string | null {
  for (const userId of selectedIds) {
    const user = users.find((candidate) => candidate.id === userId)
    if (user == null || user.role !== 'STUDENT') {
      return 'Selected people must be existing students.'
    }
  }
  return null
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
