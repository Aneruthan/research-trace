import assert from 'node:assert/strict'
import test from 'node:test'
import {
  EXTRACTING_TASKS_MESSAGE,
  NO_ACTIONABLE_TASKS_MESSAGE,
  TASK_EXTRACTION_COMPLETED_MESSAGE,
  TASK_EXTRACTOR_UNAVAILABLE_MESSAGE,
  shouldExtractTasksAfterNotesSave,
  taskExtractionFailureMessage,
  taskExtractionSuccessMessage,
} from './taskExtraction.ts'

test('new and updated meeting notes trigger task extraction', () => {
  assert.equal(shouldExtractTasksAfterNotesSave('', 'ACTION: Alex | Measure the gap'), true)
  assert.equal(shouldExtractTasksAfterNotesSave('old notes', 'ACTION: Alex | Measure the gap'), true)
  assert.equal(shouldExtractTasksAfterNotesSave('  same notes  ', 'same notes'), false)
  assert.equal(shouldExtractTasksAfterNotesSave('notes', '   '), false)
  assert.equal(shouldExtractTasksAfterNotesSave(null, undefined), false)
})

test('task extraction messages distinguish success, empty notes, and failure', () => {
  assert.equal(EXTRACTING_TASKS_MESSAGE, 'Extracting actionable tasks from meeting notes...')
  assert.equal(taskExtractionSuccessMessage(2), TASK_EXTRACTION_COMPLETED_MESSAGE)
  assert.equal(taskExtractionSuccessMessage(0), NO_ACTIONABLE_TASKS_MESSAGE)
  assert.equal(
    taskExtractionFailureMessage(Object.assign(new Error('Could not extract tasks: Gemini task extraction is not configured'), { status: 503 })),
    TASK_EXTRACTOR_UNAVAILABLE_MESSAGE,
  )
  assert.equal(taskExtractionFailureMessage(Object.assign(new Error('Could not extract tasks (500)'), { status: 500 })), 'Task extraction failed. Try again.')
  assert.match(
    taskExtractionFailureMessage(Object.assign(new Error('Could not extract tasks: Task extraction failed. The model response could not be used.'), { status: 502 })),
    /model response could not be used/,
  )
  assert.doesNotMatch(
    taskExtractionFailureMessage(Object.assign(new Error('Could not extract tasks (502)'), { status: 502 })),
    /No actionable tasks/,
  )
})
