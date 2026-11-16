import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  PROJECT_FORM_CONTROLS,
  archiveConfirmation,
  emptyProjectForm,
  projectFormIncludesExistingStudentSelection,
  projectToForm,
  toProjectWriteRequest,
  validateProjectForm,
} from './projectForm.ts'
import type { Project } from '../api/types.ts'

test('project create and edit forms do not select existing students', () => {
  assert.equal(projectFormIncludesExistingStudentSelection('create'), false)
  assert.equal(projectFormIncludesExistingStudentSelection('edit'), false)
  assert.equal((PROJECT_FORM_CONTROLS as readonly string[]).includes('Students'), false)
  const source = readFileSync(new URL('../pages/Projects.tsx', import.meta.url), 'utf8')
  assert.equal(source.includes('type="checkbox"'), false)
  assert.equal(source.includes('Select one or more existing students'), false)
  assert.equal(source.includes('membershipChanges'), false)
  assert.equal(source.includes('removeProjectMember'), false)
  assert.match(source, /formatAssignedStudents/)
})

test('create project form requires a name and rejects an inverted date range', () => {
  assert.equal(validateProjectForm(emptyProjectForm), 'Project name is required.')
  assert.equal(
    validateProjectForm({
      ...emptyProjectForm,
      name: 'GreenRoute',
      startDate: '2026-08-01',
      targetDate: '2026-07-01',
    }),
    'Start date must not be later than the target completion date.',
  )
  assert.equal(validateProjectForm({ ...emptyProjectForm, name: 'GreenRoute' }), null)
})

test('edit project form keeps existing values and sends explicit nulls', () => {
  const project: Project = {
    id: 4,
    name: 'GreenRoute',
    description: 'Routing study',
    objectives: 'Reduce emissions',
    startDate: '2026-02-01',
    targetDate: null,
    supervisorId: 8,
    status: 'ACTIVE',
  }
  const form = projectToForm(project)
  assert.equal(form.name, 'GreenRoute')
  assert.equal(form.supervisorId, '8')
  form.description = ''
  form.supervisorId = ''
  const request = toProjectWriteRequest(form)
  assert.equal(request.name, 'GreenRoute')
  assert.equal(request.description, null)
  assert.equal(request.objectives, 'Reduce emissions')
  assert.equal(request.supervisorId, null)
  assert.equal(request.startDate, '2026-02-01')
  assert.equal(request.targetDate, null)
})

test('archive confirmation explains that history is kept', () => {
  assert.match(archiveConfirmation('GreenRoute'), /Archive GreenRoute/)
  assert.match(archiveConfirmation('GreenRoute'), /will be kept/)
})
