import assert from 'node:assert/strict'
import test from 'node:test'
import type { ResearchDecision } from '../../api/research'
import { buildDecisionChains, reviewOutcomeMessage } from './researchDisplay.ts'

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
  assert.match(reviewOutcomeMessage('EDITED'), /no decisions, experiments, or results were added/)
  assert.doesNotMatch(reviewOutcomeMessage('EDITED'), /now added/)
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

test('buildDecisionChains starts a chain at a decision whose predecessor is missing', () => {
  const chains = buildDecisionChains([decision(5, 4), decision(6, 5)])
  assert.deepEqual(chains.map((chain) => chain.map((d) => d.id)), [[5, 6]])
})
