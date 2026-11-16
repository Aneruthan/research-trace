import type { ProjectStatus } from '../api/types.ts'

export const PROJECT_DASHBOARD_PATH = '/'

export type ProjectCardAction = 'Open' | 'Edit'

export function projectCardActions(project: {
  id: number | null
  status: ProjectStatus | null
}): ProjectCardAction[] {
  if (project.id == null || project.status === 'ARCHIVED') {
    return []
  }
  return ['Open', 'Edit']
}

export function openProjectDestination(projectId: number): { projectId: number; path: string } {
  return { projectId, path: PROJECT_DASHBOARD_PATH }
}
