import { API_BASE_URL, parseJsonResponse, readErrorMessage } from './config.ts'
import type { Meeting } from './types.ts'

export type ResearchReviewStatus = 'PENDING' | 'ACCEPTED' | 'EDITED' | 'REJECTED'
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
  | 'CHANGE'
  | 'TASK_COMPLETED'
export type ResearchChangeStatus = 'FLAGGED' | 'REVIEWED' | 'DISMISSED'

// Extracted DTO types from AI
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

// Persistent Research Domain Types
export type ResearchDecision = {
  id: number | null
  projectId: number | null
  meetingId: number | null
  extractionReviewId?: number | null
  title: string | null
  decisionText: string | null
  rationale: string | null
  status: ResearchDecisionStatus | null
  supersedesDecisionId: number | null
  sourceReference: string | null
  decidedAt: string | null
  createdAt?: string | null
  createdByUserId?: number | null
}

export type Experiment = {
  id: number | null
  projectId: number | null
  researchDecisionId: number | null
  meetingId?: number | null
  title: string | null
  hypothesis: string | null
  method: string | null
  status: ExperimentStatus | null
  sourceReference: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt?: string | null
  createdByUserId?: number | null
}

export type Result = {
  id: number | null
  projectId: number | null
  experimentId: number | null
  meetingId?: number | null
  summary: string | null
  dataOrMetric: string | null
  comparison?: string | null
  sourceReference: string | null
  observedAt: string | null
  createdAt?: string | null
  createdByUserId?: number | null
}

export type ResearchEvent = {
  id: number | null
  projectId: number | null
  eventType: ResearchEventType | null
  title: string | null
  description: string | null
  occurredAt: string | null
  createdAt?: string | null
  meetingId: number | null
  taskId?: number | null
  researchDecisionId: number | null
  experimentId: number | null
  resultId: number | null
  researchChangeId?: number | null
  sourceReference: string | null
  createdByUserId?: number | null
}

export type ResearchChange = {
  id: number
  projectId: number
  meetingId: number | null
  changeType: string
  title: string
  previousApproach: string | null
  newApproach: string | null
  reason: string | null
  sourceReference: string | null
  status: ResearchChangeStatus
  detectedAt: string
  createdAt: string
}

export type ExperimentEvidence = {
  experiment: Experiment
  results: Result[]
}

export type DecisionLineageView = {
  decision: ResearchDecision
  predecessorDecision: ResearchDecision | null
  successorDecision: ResearchDecision | null
  sourceMeeting: Meeting | null
  experiments: ExperimentEvidence[]
}

export type ResearchTraceSummary = {
  totalDecisions: number
  activeDecisions: number
  totalExperiments: number
  completedExperiments: number
  totalResults: number
  flaggedChanges: number
  decisions: ResearchDecision[]
  experiments: Experiment[]
  results: Result[]
  timeline: ResearchEvent[]
  changes: ResearchChange[]
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

// AI Extraction & Human Review API
export async function extractResearch(meetingId: number): Promise<MeetingExtraction> {
  const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/research-extraction`, {
    method: 'POST',
  })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not extract research information'), response.status)
  }
  return parseJsonResponse<MeetingExtraction>(response)
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
  return parseJsonResponse<ExtractionReview>(response)
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
    throw new Error(await readErrorMessage(response, fallback))
  }
  return parseJsonResponse<T>(response, fallback)
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

// Research Trace & Domain APIs (/api/projects/{projectId}/research)

export async function getResearchSummary(
  projectId: number,
  signal?: AbortSignal,
): Promise<ResearchTraceSummary> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/research/summary`, { signal })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load research summary'), response.status)
  }
  return response.json() as Promise<ResearchTraceSummary>
}

export async function getResearchDecisions(
  projectId: number,
  status?: ResearchDecisionStatus,
  signal?: AbortSignal,
): Promise<ResearchDecision[]> {
  const url = status
    ? `${API_BASE_URL}/api/projects/${projectId}/research/decisions?status=${status}`
    : `${API_BASE_URL}/api/projects/${projectId}/research/decisions`
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load decisions'), response.status)
  }
  return response.json() as Promise<ResearchDecision[]>
}

export async function createResearchDecision(
  projectId: number,
  request: {
    title: string
    decisionText?: string
    rationale?: string
    meetingId?: number
    sourceReference?: string
    decidedAt?: string
  },
): Promise<ResearchDecision> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/research/decisions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not create research decision'), response.status)
  }
  return response.json() as Promise<ResearchDecision>
}

export async function createProjectExperiment(
  projectId: number,
  request: { title: string; researchDecisionId?: number | null },
): Promise<Experiment> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/research/experiments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not create experiment'), response.status)
  }
  return parseJsonResponse<Experiment>(response, 'Could not create experiment')
}

export async function updateExperimentDecision(
  projectId: number,
  experimentId: number,
  researchDecisionId: number | null,
): Promise<Experiment> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/research/experiments/${experimentId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ researchDecisionId }),
    },
  )
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not update the experiment link'), response.status)
  }
  return parseJsonResponse<Experiment>(response, 'Could not update the experiment link')
}

export async function supersedeResearchDecision(
  projectId: number,
  decisionId: number,
  request: {
    title: string
    rationale: string
    decisionText?: string
    meetingId?: number
    sourceReference?: string
    decidedAt?: string
  },
): Promise<ResearchDecision> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/research/decisions/${decisionId}/supersede`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
  )
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not supersede research decision'), response.status)
  }
  return response.json() as Promise<ResearchDecision>
}

export async function getDecisionLineage(
  projectId: number,
  decisionId: number,
  signal?: AbortSignal,
): Promise<DecisionLineageView> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/research/decisions/${decisionId}/lineage`,
    { signal },
  )
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load decision lineage'), response.status)
  }
  return response.json() as Promise<DecisionLineageView>
}

export async function getResearchTimeline(
  projectId: number,
  signal?: AbortSignal,
): Promise<ResearchEvent[]> {
  const response = await fetch(`${API_BASE_URL}/api/projects/${projectId}/research/timeline`, { signal })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load research timeline'), response.status)
  }
  return response.json() as Promise<ResearchEvent[]>
}

export async function getResearchChanges(
  projectId: number,
  status?: ResearchChangeStatus,
  signal?: AbortSignal,
): Promise<ResearchChange[]> {
  const url = status
    ? `${API_BASE_URL}/api/projects/${projectId}/research/changes?status=${status}`
    : `${API_BASE_URL}/api/projects/${projectId}/research/changes`
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not load research changes'), response.status)
  }
  return response.json() as Promise<ResearchChange[]>
}

export async function updateResearchChangeStatus(
  projectId: number,
  changeId: number,
  status: ResearchChangeStatus,
): Promise<ResearchChange> {
  const response = await fetch(
    `${API_BASE_URL}/api/projects/${projectId}/research/changes/${changeId}/status?status=${status}`,
    {
      method: 'PUT',
    },
  )
  if (!response.ok) {
    throw new ApiError(await failureMessage(response, 'Could not update change status'), response.status)
  }
  return response.json() as Promise<ResearchChange>
}

async function failureMessage(response: Response, fallback: string): Promise<string> {
  if (response.status === 503) {
    return 'Gemini research extraction is not configured (503).'
  }
  return readErrorMessage(response, fallback)
}
