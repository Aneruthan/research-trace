import type { Project, ProjectWriteRequest } from '../api/types.ts'

export type ProjectFormValues = {
  name: string
  description: string
  objectives: string
  startDate: string
  targetDate: string
  supervisorId: string
}

export const emptyProjectForm: ProjectFormValues = {
  name: '',
  description: '',
  objectives: '',
  startDate: '',
  targetDate: '',
  supervisorId: '',
}

export function projectToForm(project: Project): ProjectFormValues {
  return {
    name: project.name ?? '',
    description: project.description ?? '',
    objectives: project.objectives ?? '',
    startDate: project.startDate ?? '',
    targetDate: project.targetDate ?? '',
    supervisorId: project.supervisorId == null ? '' : String(project.supervisorId),
  }
}

export function validateProjectForm(values: ProjectFormValues): string | null {
  if (values.name.trim() === '') {
    return 'Project name is required.'
  }
  if (values.name.trim().length > 255) {
    return 'Project name must be at most 255 characters.'
  }
  if (values.description.trim().length > 255) {
    return 'Description must be at most 255 characters.'
  }
  if (values.objectives.trim().length > 4000) {
    return 'Research objectives must be at most 4000 characters.'
  }
  if (values.startDate !== '' && values.targetDate !== '' && values.startDate > values.targetDate) {
    return 'Start date must not be later than the target completion date.'
  }
  if (values.supervisorId !== '' && !/^[1-9]\d*$/.test(values.supervisorId)) {
    return 'Choose a supervisor from the list.'
  }
  return null
}

export function toProjectWriteRequest(values: ProjectFormValues): ProjectWriteRequest {
  return {
    name: values.name.trim(),
    description: values.description.trim() === '' ? null : values.description.trim(),
    objectives: values.objectives.trim() === '' ? null : values.objectives.trim(),
    startDate: values.startDate === '' ? null : values.startDate,
    targetDate: values.targetDate === '' ? null : values.targetDate,
    supervisorId: values.supervisorId === '' ? null : Number(values.supervisorId),
  }
}

export function archiveConfirmation(projectName: string | null): string {
  const name = projectName?.trim() ? projectName.trim() : 'this project'
  return `Archive ${name}? Meetings, tasks, milestones, and research history will be kept. The project will leave the active project list.`
}
