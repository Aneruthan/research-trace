import assert from 'node:assert/strict'
import test from 'node:test'
import { openProjectDestination, projectCardActions, PROJECT_DASHBOARD_PATH } from './projectCard.ts'

test('project cards show Edit and Open and do not offer Archive', () => {
  const actions = projectCardActions({ id: 4, status: 'ACTIVE' })
  assert.deepEqual(actions, ['Open', 'Edit'])
  assert.equal(actions.includes('Archive' as never), false)
  assert.equal(projectCardActions({ id: 4, status: 'COMPLETED' }).includes('Open'), true)
  assert.deepEqual(projectCardActions({ id: null, status: 'ACTIVE' }), [])
  assert.deepEqual(projectCardActions({ id: 4, status: 'ARCHIVED' }), [])
})

test('Open selects the project and goes to its dashboard', () => {
  const destination = openProjectDestination(7)
  assert.equal(destination.projectId, 7)
  assert.equal(destination.path, PROJECT_DASHBOARD_PATH)
  assert.equal(destination.path, '/')
})
