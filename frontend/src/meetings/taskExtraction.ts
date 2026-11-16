import { extractTaskSuggestions } from '../api/taskSuggestions.ts'
import type { TaskSuggestion } from '../api/types.ts'

export const EXTRACTING_TASKS_MESSAGE = 'Extracting actionable tasks from meeting notes...'
export const TASK_EXTRACTION_COMPLETED_MESSAGE = 'Task extraction completed. Review the suggestions below.'
export const NO_ACTIONABLE_TASKS_MESSAGE = 'No actionable tasks were identified in these meeting notes.'
export const TASK_EXTRACTOR_UNAVAILABLE_MESSAGE =
  'Task extraction is currently unavailable. Check the AI configuration and try again.'

const inFlight = new Set<string>()

export function shouldExtractTasksAfterNotesSave(
  previousNotes: string | null | undefined,
  savedNotes: string | null | undefined,
): boolean {
  const next = (savedNotes ?? '').trim()
  if (next.length === 0) {
    return false
  }
  return next !== (previousNotes ?? '').trim()
}

export function taskExtractionSuccessMessage(count: number): string {
  return count === 0 ? NO_ACTIONABLE_TASKS_MESSAGE : TASK_EXTRACTION_COMPLETED_MESSAGE
}

export function taskExtractionFailureMessage(error: unknown): string {
  const status = errorStatus(error)
  const message = error instanceof Error ? error.message : ''
  if (status === 503 || /not configured|currently unavailable/i.test(message)) {
    return TASK_EXTRACTOR_UNAVAILABLE_MESSAGE
  }
  const detail = message.replace(/^Could not extract tasks:?\s*/i, '').trim()
  if (detail.length === 0 || detail === 'Could not extract tasks' || /^\(\d+\)$/.test(detail)) {
    return 'Task extraction failed. Try again.'
  }
  return `Task extraction failed. ${detail}`
}

export type TaskExtractionRun =
  | { skipped: true }
  | { skipped: false; suggestions: TaskSuggestion[]; message: string }

export async function extractSavedMeetingTasks(
  projectId: number,
  meetingId: number,
): Promise<TaskExtractionRun> {
  const key = `${projectId}:${meetingId}`
  if (inFlight.has(key)) {
    return { skipped: true }
  }
  inFlight.add(key)
  try {
    const suggestions = await extractTaskSuggestions(projectId, meetingId)
    return {
      skipped: false,
      suggestions,
      message: taskExtractionSuccessMessage(suggestions.length),
    }
  } finally {
    inFlight.delete(key)
  }
}

function errorStatus(error: unknown): number | undefined {
  if (typeof error === 'object' && error !== null && 'status' in error) {
    const status = (error as { status?: unknown }).status
    return typeof status === 'number' ? status : undefined
  }
  return undefined
}
