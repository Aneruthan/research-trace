import type { Project } from '../api/types.ts'

export const ACTIVE_PROJECT_STORAGE_KEY = 'researchtrace.activeProjectId'

export function readStoredProjectId(raw: string | null): number | null {
  if (raw == null) {
    return null
  }
  const trimmed = raw.trim()
  if (!/^[1-9]\d*$/.test(trimmed)) {
    return null
  }
  const id = Number(trimmed)
  return Number.isSafeInteger(id) ? id : null
}

export function selectableProjects(projects: Project[]): Project[] {
  return projects
    .filter((project) => project.id != null && project.status !== 'ARCHIVED')
    .sort((left, right) => (left.id as number) - (right.id as number))
}

export function resolveActiveProjectId(projects: Project[], storedId: number | null): number | null {
  const selectable = selectableProjects(projects)
  if (storedId != null && selectable.some((project) => project.id === storedId)) {
    return storedId
  }
  return selectable[0]?.id ?? null
}

export function visibleProjects(projects: Project[], showArchived: boolean): Project[] {
  return projects
    .filter((project) => showArchived || project.status !== 'ARCHIVED')
    .sort((left, right) => (left.id ?? 0) - (right.id ?? 0))
}

export function isCurrentProjectResponse(requestedProjectId: number, currentProjectId: number | null): boolean {
  return currentProjectId === requestedProjectId
}

export function meetingBelongsToProject(
  meetingProjectId: number | null | undefined,
  activeProjectId: number | null,
): boolean {
  return activeProjectId != null && meetingProjectId === activeProjectId
}
