# Research Traceability API Contract

This document describes the API contract, data structures, and lifecycle workflows for the **Research Traceability & Project Dashboard** module in ResearchTrace.

---

## 1. Overview & Research Trace Concept

The Research Trace module maintains a chronological, immutable ledger of all scientific inquiry and lifecycle events across a research project. It records research questions, hypotheses, meetings, decisions, supersessions, experiments, results, and research-linked task completions.

---

## 2. Research Event Model

### 2.1 ResearchEvent Entity

Represents a single point on the chronological research timeline.

| Field | Type | Description |
|---|---|---|
| `id` | Long (generated) | Unique event identifier |
| `projectId` | Long | Project containing the event |
| `eventType` | enum | `MEETING`, `DECISION`, `EXPERIMENT`, `RESULT`, `OBSERVATION`, `HYPOTHESIS`, `RESEARCH_QUESTION`, `TASK_COMPLETED` |
| `title` | string | Concise event headline |
| `description` | string | Detailed summary or context |
| `occurredAt` | datetime | Timestamp of the underlying real-world event |
| `createdAt` | datetime | Audit timestamp when the event was recorded |
| `createdByUserId` | Long (nullable) | User attribution (caller-provided) |
| `meetingId` | Long (nullable) | Link to originating `Meeting` |
| `researchDecisionId` | Long (nullable) | Link to associated `ResearchDecision` |
| `experimentId` | Long (nullable) | Link to associated `Experiment` |
| `resultId` | Long (nullable) | Link to associated `Result` |
| `taskId` | Long (nullable) | Link to associated `Task` (Stage 2 addition) |
| `sourceReference` | string (nullable) | External citation, notebook link, commit hash, or transcript path |

### 2.2 Event Ordering

Events are ordered chronologically by `occurredAt ASC`, with `createdAt ASC` and `id ASC` as deterministic tie-breakers.

---

## 3. Timeline Endpoints

### 3.1 Fetch Project Timeline

`GET /api/projects/{projectId}/events` (or `GET /api/projects/{projectId}/timeline`)

Returns the ordered chronological timeline of all research events for `{projectId}`.

**Query Parameters:**
- `type` (optional): Filter by `ResearchEventType` (e.g. `?type=TASK_COMPLETED`).
- `limit` (optional): Maximum number of events to return.

**Response Body:**
```json
[
  {
    "id": 401,
    "projectId": 1,
    "eventType": "MEETING",
    "title": "Supervision Meeting #3",
    "description": "Discussed baseline evaluation results and tokenization strategies.",
    "occurredAt": "2026-10-25T14:00:00Z",
    "meetingId": 3,
    "researchDecisionId": null,
    "experimentId": null,
    "taskId": null
  },
  {
    "id": 402,
    "projectId": 1,
    "eventType": "DECISION",
    "title": "Switch to BPE Tokenizer",
    "description": "Decided to adopt Byte-Pair Encoding based on preliminary vocabulary coverage.",
    "occurredAt": "2026-10-25T15:00:00Z",
    "meetingId": 3,
    "researchDecisionId": 4,
    "experimentId": null,
    "taskId": null
  },
  {
    "id": 403,
    "projectId": 1,
    "eventType": "TASK_COMPLETED",
    "title": "Task Completed: Evaluate Qwen-7B on baseline dataset",
    "description": "Completed task 'Evaluate Qwen-7B on baseline dataset' linked to Decision #4 and Experiment #2",
    "occurredAt": "2026-10-31T15:00:00Z",
    "meetingId": null,
    "researchDecisionId": 4,
    "experimentId": 2,
    "taskId": 10
  }
]
```

### 3.2 Add Custom Timeline Event

`POST /api/projects/{projectId}/events`

Manually records an observation, hypothesis, or external milestone onto the timeline.

---

## 4. Task Completion Timeline Integration (`TASK_COMPLETED`)

### 4.1 Automated Trigger Conditions

A `TASK_COMPLETED` research event is automatically emitted when:
1. A task transitions to `TaskStatus.COMPLETED` via `TaskService.updateTaskStatus(...)`.
2. The task has at least one research association: `researchDecisionId != null` OR `experimentId != null`.
3. The previous status was NOT already `COMPLETED` (prevents duplicate emissions on repeated requests).

### 4.2 Single Event with Dual References

If a task is linked to **both** a research decision and an experiment:
- Exactly **one** `ResearchEvent` is generated.
- The event records both `researchDecisionId` and `experimentId` alongside `taskId`.
- The event description explicitly references both motivating artifacts.

### 4.3 Transactional Rollback Guarantee

Event creation runs in the same `@Transactional` boundary as task status updates and `TaskHistory` persistence:
- If event persistence throws an exception, the entire transaction rolls back.
- The task status remains at its previous value, and no orphaned history record is written.

---

## 5. Project Dashboard & Research Metrics

`GET /api/projects/{projectId}/dashboard`

Returns aggregated health, progress, and research activity indicators strictly scoped to `{projectId}`.

**Response Structure:**
```json
{
  "projectId": 1,
  "projectName": "Neural Code Understanding",
  "projectProgress": 65.50,
  "milestones": [
    {
      "id": 12,
      "title": "Dataset Curation",
      "progress": 100.00,
      "weight": 30,
      "status": "COMPLETED"
    }
  ],
  "tasksSummary": {
    "totalTasks": 14,
    "completedTasks": 9,
    "inProgressTasks": 3,
    "todoTasks": 2
  },
  "researchMetrics": {
    "totalDecisions": 6,
    "activeDecisions": 5,
    "supersededDecisions": 1,
    "totalExperiments": 8,
    "completedExperiments": 5,
    "totalResults": 12,
    "totalEvents": 34,
    "totalChanges": 2
  }
}
```

### 5.1 ResearchMetrics Definitions

- `totalDecisions`: Count of all decisions belonging to `{projectId}`.
- `activeDecisions`: Count of decisions with `status == ACTIVE`.
- `supersededDecisions`: Count of decisions with `status == SUPERSEDED`.
- `totalExperiments`: Count of all experiments belonging to `{projectId}`.
- `completedExperiments`: Count of experiments with `status == COMPLETED`.
- `totalResults`: Count of all empirical results belonging to `{projectId}`.
- `totalEvents`: Total count of chronological entries in `research_event` for `{projectId}`.
- `totalChanges`: Total recorded research evolution steps (superseded decisions and experiment status changes).

All metrics are computed via project-scoped repository count queries (`WHERE project_id = :projectId`) without in-memory full-table scans.

---

## 6. Review Acceptance Provenance Integration

When supervision meeting notes or AI extractions are accepted via `AcceptedReviewRecorder`:
1. `saveExperiments(...)`: Sets `meetingId` from the originating `ExtractionReview`.
2. `saveResults(...)`: Sets `meetingId` from the originating `ExtractionReview`.
3. `createTaskFromSuggestion(...)`: Propagates `meetingId`, `researchDecisionId`, and `experimentId` into the new `Task` entity.

This guarantees that any subsequent query from task to decision, experiment, result, or meeting can traverse the entire provenance graph bidirectionally.
