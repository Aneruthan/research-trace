import { API_BASE_URL } from './config'

export type ResearchReviewStatus = 'PENDING' | 'ACCEPTED' | 'EDITED' | 'REJECTED'

export type ExtractedResearchTask = {
  title: string | null
  description: string | null
  assignee: number | null
  deadline: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedDecision = {
  title: string | null
  description: string | null
  madeBy: string | null
  reason: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedExperiment = {
  title: string | null
  description: string | null
  objective: string | null
  variables: string | null
  expectedOutcome: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedResult = {
  description: string | null
  experiment: string | null
  metrics: string | null
  comparison: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedDeadline = {
  description: string | null
  date: string | null
  relativeDate: string | null
  associatedTask: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedResearchEntity = {
  name: string | null
  type: string | null
  description: string | null
  context: string | null
  source: number | null
  confidence: number | null
}

export type ExtractedResearchChange = {
  description: string | null
  previousApproach: string | null
  newApproach: string | null
  reason: string | null
  source: number | null
  confidence: number | null
}

export type MeetingExtraction = {
  tasks: ExtractedResearchTask[] | null
  decisions: ExtractedDecision[] | null
  experiments: ExtractedExperiment[] | null
  results: ExtractedResult[] | null
  deadlines: ExtractedDeadline[] | null
  researchEntities: ExtractedResearchEntity[] | null
  researchChanges: ExtractedResearchChange[] | null
}

export type ExtractionReview = {
  id: number | null
  meetingId: number | null
  status: ResearchReviewStatus | null
  extractionJson: string | null
  createdAt: string | null
}

export type ResearchDecisionStatus = 'ACTIVE' | 'SUPERSEDED'

export type ExperimentStatus = 'PLANNED' | 'RUNNING' | 'COMPLETED'

export type ResearchEventType =
  | 'MEETING'
  | 'DECISION'
  | 'EXPERIMENT'
  | 'RESULT'
  | 'OBSERVATION'
  | 'HYPOTHESIS'
  | 'RESEARCH_QUESTION'

export type ResearchDecision = {
  id: number | null
  projectId: number | null
  meetingId: number | null
  extractionReviewId: number | null
  title: string | null
  decisionText: string | null
  rationale: string | null
  status: ResearchDecisionStatus | null
  supersedesDecisionId: number | null
  decidedAt: string | null
  sourceReference: string | null
}

export type Experiment = {
  id: number | null
  projectId: number | null
  researchDecisionId: number | null
  title: string | null
  hypothesis: string | null
  method: string | null
  status: ExperimentStatus | null
  startedAt: string | null
  completedAt: string | null
  sourceReference: string | null
}

export type Result = {
  id: number | null
  projectId: number | null
  experimentId: number | null
  summary: string | null
  dataOrMetric: string | null
  observedAt: string | null
  sourceReference: string | null
}

export type ResearchEvent = {
  id: number | null
  projectId: number | null
  eventType: ResearchEventType | null
  occurredAt: string | null
  title: string | null
  description: string | null
  meetingId: number | null
  researchDecisionId: number | null
  experimentId: number | null
  resultId: number | null
  sourceReference: string | null
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export async function extractResearch(meetingId: number): Promise<MeetingExtraction> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/research-extraction`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not extract research information'), response.status)
  }
  return response.json() as Promise<MeetingExtraction>
}

export async function submitResearchReview(
  meetingId: number,
  status: ResearchReviewStatus,
  extraction: MeetingExtraction,
): Promise<ExtractionReview> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, extraction }),
  })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not save the research review'), response.status)
  }
  return response.json() as Promise<ExtractionReview>
}

export async function getProjectResearchDecisions(
  projectId: number,
  signal?: AbortSignal,
): Promise<ResearchDecision[]> {
  return getJson(`/api/projects/${projectId}/research-decisions`, signal, `Could not load research decisions for project ${projectId}`)
}

export async function getProjectExperiments(
  projectId: number,
  signal?: AbortSignal,
): Promise<Experiment[]> {
  return getJson(`/api/projects/${projectId}/experiments`, signal, `Could not load experiments for project ${projectId}`)
}

export async function getProjectResults(
  projectId: number,
  signal?: AbortSignal,
): Promise<Result[]> {
  return getJson(`/api/projects/${projectId}/results`, signal, `Could not load results for project ${projectId}`)
}

export async function getProjectResearchEvents(
  projectId: number,
  signal?: AbortSignal,
): Promise<ResearchEvent[]> {
  return getJson(`/api/projects/${projectId}/research-events`, signal, `Could not load research events for project ${projectId}`)
}

async function getJson<T>(path: string, signal: AbortSignal | undefined, fallback: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, { signal })
  if (!response.ok) {
    throw new Error(`${fallback} (${response.status})`)
  }
  return response.json() as Promise<T>
}

export async function getResearchReviews(
  meetingId: number,
  signal?: AbortSignal,
): Promise<ExtractionReview[]> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/reviews`, { signal })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load research reviews'), response.status)
  }
  return response.json() as Promise<ExtractionReview[]>
}

async function failureMessage(response: Response, fallback: string): Promise<string> {
  if (response.status === 503) {
    return 'Gemini research extraction is not configured (503).'
  }
  return `${fallback} (${response.status})`
}
