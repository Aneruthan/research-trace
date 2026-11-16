import test from 'node:test'
import assert from 'node:assert/strict'
import type { Project } from '../api/types.ts'
import {
  ACTIVE_PROJECT_STORAGE_KEY,
  isCurrentProjectResponse,
  meetingBelongsToProject,
  readStoredProjectId,
  resolveActiveProjectId,
  selectableProjects,
  visibleProjects,
} from './projectSelection.ts'

function project(id: number, status: Project['status'], name = `Project ${id}`): Project {
  return {
    id,
    name,
    description: null,
    objectives: null,
    startDate: null,
    targetDate: null,
    supervisorId: null,
    status,
  }
}

test('project list keeps current projects and can include archived ones', () => {
  const projects = [project(2, 'ACTIVE', 'GreenRoute'), project(1, 'ARCHIVED', 'Old study')]
  assert.deepEqual(visibleProjects(projects, false).map((item) => item.id), [2])
  assert.deepEqual(visibleProjects(projects, true).map((item) => item.id), [1, 2])
})

test('project selector excludes archived projects', () => {
  const selectable = selectableProjects([
    project(3, 'ARCHIVED'),
    project(1, 'ACTIVE', 'Multimodal Biomedical AI'),
    project(2, 'COMPLETED', 'GreenRoute'),
  ])
  assert.deepEqual(selectable.map((item) => item.id), [1, 2])
})

test('switching selection resolves the chosen active project', () => {
  const projects = [project(1, 'ACTIVE'), project(2, 'ACTIVE', 'GreenRoute')]
  assert.equal(resolveActiveProjectId(projects, 2), 2)
})

test('persisted selection is restored only as a numeric project id', () => {
  assert.equal(readStoredProjectId('4'), 4)
  assert.equal(ACTIVE_PROJECT_STORAGE_KEY, 'researchtrace.activeProjectId')
  const storage = new Map<string, string>()
  storage.set(ACTIVE_PROJECT_STORAGE_KEY, '4')
  assert.equal(readStoredProjectId(storage.get(ACTIVE_PROJECT_STORAGE_KEY) ?? null), 4)
})

test('an invalid stored project id does not fall back to project 1', () => {
  const projects = [project(2, 'ACTIVE', 'GreenRoute'), project(1, 'ACTIVE', 'Multimodal Biomedical AI')]
  assert.equal(readStoredProjectId('nope'), null)
  assert.equal(readStoredProjectId(''), null)
  assert.equal(resolveActiveProjectId(projects, 99), 1)
  assert.equal(resolveActiveProjectId(projects, null), 1)
  assert.notEqual(resolveActiveProjectId([project(4, 'ACTIVE', 'Crop Disease Detection')], 1), 1)
})

test('an archived stored project is not restored', () => {
  const projects = [project(1, 'ARCHIVED'), project(5, 'ACTIVE', 'Crop Disease Detection')]
  assert.equal(resolveActiveProjectId(projects, 1), 5)
})

test('an empty project list selects nothing', () => {
  assert.equal(resolveActiveProjectId([], 1), null)
  assert.equal(resolveActiveProjectId([project(1, 'ARCHIVED')], null), null)
})

test('dashboard, meetings, tasks, milestones, and research reload ignore a stale project response', () => {
  assert.equal(isCurrentProjectResponse(2, 2), true)
  assert.equal(isCurrentProjectResponse(1, 2), false)
  assert.equal(isCurrentProjectResponse(2, null), false)
})

test('meeting details rejects a meeting from another project', () => {
  assert.equal(meetingBelongsToProject(2, 2), true)
  assert.equal(meetingBelongsToProject(1, 2), false)
  assert.equal(meetingBelongsToProject(null, 2), false)
  assert.equal(meetingBelongsToProject(2, null), false)
})
