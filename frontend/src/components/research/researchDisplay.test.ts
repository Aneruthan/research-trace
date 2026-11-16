import assert from 'node:assert/strict'
import test from 'node:test'
import type { ResearchDecision } from '../../api/research'
import { buildDecisionChains, editExtractionList, extractionOrigin, incompleteExtractionIssues, reviewOutcomeMessage } from './researchDisplay.ts'

function decision(id: number, supersedesDecisionId: number | null, title = `Decision ${id}`): ResearchDecision {
  return {
    id,
    projectId: 1,
    meetingId: null,
    title,
    decisionText: null,
    rationale: null,
    status: 'ACTIVE',
    supersedesDecisionId,
    sourceReference: null,
    decidedAt: null,
  }
}

test('reviewOutcomeMessage only claims persisted records for ACCEPTED reviews', () => {
  assert.match(reviewOutcomeMessage('ACCEPTED'), /now added to the research trace/)
  assert.match(reviewOutcomeMessage('EDITED'), /Edited extraction saved/)
  assert.match(reviewOutcomeMessage('EDITED'), /Accept this version/)
  assert.match(reviewOutcomeMessage('REJECTED'), /No records were added/)
  assert.match(reviewOutcomeMessage('PENDING'), /No records were added/)
})

test('buildDecisionChains keeps a decision without successors as a single-item chain', () => {
  const chains = buildDecisionChains([decision(1, null)])
  assert.deepEqual(chains.map((chain) => chain.map((d) => d.id)), [[1]])
})

test('buildDecisionChains follows a supersession chain to the end', () => {
  const chains = buildDecisionChains([decision(3, 2), decision(1, null), decision(2, 1), decision(9, null)])
  assert.deepEqual(chains.map((chain) => chain.map((d) => d.id)), [[1, 2, 3], [9]])
})

test('incomplete extracted items stay visible and name the missing field', () => {
  const issues = incompleteExtractionIssues({
    decisions: [{ title: '  ' }],
    experiments: [{ title: 'Bias audit' }],
    results: [{ description: '' }],
  })
  assert.deepEqual(issues, [
    'Decision 1 needs a title before it can be accepted.',
    'Result 1 needs a description before it can be accepted.',
  ])
  assert.equal(incompleteExtractionIssues({ decisions: [{ title: 'Keep the baseline' }] }).length, 0)
})

test('edited extraction keeps the original title and replaces only the edited field', () => {
  const edited = editExtractionList(
    [{ title: 'Keep the baseline', description: 'Original', madeBy: null, reason: null, source: null, confidence: null }],
    0,
    { title: 'Use a balanced split' },
  )
  assert.equal(edited[0].title, 'Use a balanced split')
  assert.equal(edited[0].description, 'Original')
  assert.equal(extractionOrigin('{"title":"Keep"}', '{"title":"Keep"}'), 'ai')
  assert.equal(extractionOrigin('{"title":"Keep"}', '{"title":"Split"}'), 'edited')
  assert.equal(extractionOrigin(null, '{"title":"Split"}'), 'unknown')
})

test('buildDecisionChains starts a chain at a decision whose predecessor is missing', () => {
  const chains = buildDecisionChains([decision(5, 4), decision(6, 5)])
  assert.deepEqual(chains.map((chain) => chain.map((d) => d.id)), [[5, 6]])
})
