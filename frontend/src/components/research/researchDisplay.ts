import type { ResearchDecision, ResearchReviewStatus } from '../../api/research'

export function reviewOutcomeMessage(status: ResearchReviewStatus): string {
  switch (status) {
    case 'ACCEPTED':
      return 'Research review saved as ACCEPTED. Verified decisions, experiments, and results are now added to the research trace!'
    case 'EDITED':
      return 'Research review saved as EDITED. Only direction changes were checked and flagged; no decisions, experiments, or results were added to the research trace.'
    default:
      return `Research review saved as ${status}. No records were added to the permanent trace.`
  }
}

/**
 * Groups decisions into supersession chains, oldest first. A chain starts at a decision that
 * supersedes nothing (or whose predecessor is not in the list) and follows each successor.
 */
export function buildDecisionChains(decisions: ResearchDecision[]): ResearchDecision[][] {
  const knownIds = new Set(decisions.map((decision) => decision.id).filter((id): id is number => id != null))
  const successorOf = new Map<number, ResearchDecision>()
  for (const decision of decisions) {
    const predecessorId = decision.supersedesDecisionId
    if (predecessorId != null && !successorOf.has(predecessorId)) {
      successorOf.set(predecessorId, decision)
    }
  }

  const roots = decisions.filter(
    (decision) => decision.supersedesDecisionId == null || !knownIds.has(decision.supersedesDecisionId),
  )
  return roots.map((root) => {
    const chain = [root]
    const visited = new Set<number>()
    let current = root
    while (current.id != null && !visited.has(current.id)) {
      visited.add(current.id)
      const successor = successorOf.get(current.id)
      if (successor == null) {
        break
      }
      chain.push(successor)
      current = successor
    }
    return chain
  })
}
