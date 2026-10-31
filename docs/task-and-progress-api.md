# Task and Progress API Contract

This document defines the API contract for the **Task & Progress Management** module in ResearchTrace, including Stage 2 enhancements for task provenance, lifecycle audit history, research artifact linking, and project-scoped progress derivation.

---

## 1. Domain Model

### 1.1 Task Entity

A task represents an actionable unit of work assigned to a project member.

| Field | Type | Required | Description |
|---|---|---:|---|
| `id` | Long | generated | Unique task identifier |
| `projectId` | Long | yes | Project containing the task |
| `milestoneId` | Long | no | Shared milestone to which the task contributes |
| `assignedToUserId` | Long | yes | Primary student assignee responsible for the task |
| `meetingId` | Long | no | Originating supervision meeting provenance |
| `taskSuggestionId` | Long | no | Originating AI task suggestion reference |
| `researchDecisionId` | Long | no | Linked research decision motivating this task |
| `experimentId` | Long | no | Linked experiment validated or supported by this task |
| `title` | string | yes | Short descriptive title (non-blank) |
| `description` | string | no | Detailed task instructions or context |
| `status` | enum | yes | `TODO`, `IN_PROGRESS`, or `COMPLETED` |
| `dueDate` | date | no | Expected completion deadline |
| `createdAt` | datetime | generated | Creation audit timestamp |
| `updatedAt` | datetime | generated | Last update audit timestamp |

### 1.2 TaskHistory Entity

Records an immutable audit trail of every status transition for a task.

| Field | Type | Description |
|---|---|---|
| `id` | Long (generated) | Unique history entry identifier |
| `taskId` | Long | Reference to parent `Task.id` |
| `previousStatus` | string | Prior status (e.g., `TODO`, `IN_PROGRESS`, or `null` at initial creation) |
| `newStatus` | string | Updated status (`TODO`, `IN_PROGRESS`, `COMPLETED`) |
| `changedByUserId` | Long (nullable) | User ID of the actor initiating the transition (caller-provided) |
| `changedAt` | datetime | Timestamp when the transition occurred |

---

## 2. Task Endpoints

### 2.1 Create a Task

`POST /api/projects/{projectId}/tasks`

Creates a new task within the specified project.

**Request Body:**
```json
{
  "milestoneId": 12,
  "assignedToUserId": 7,
  "meetingId": 3,
  "taskSuggestionId": 15,
  "researchDecisionId": 4,
  "experimentId": 2,
  "title": "Evaluate Qwen-7B on baseline dataset",
  "description": "Run baseline evaluation script and log accuracy/F1 metrics.",
  "status": "TODO",
  "dueDate": "2026-11-15"
}
```

**Validation Rules:**
- `title` must not be blank.
- `assignedToUserId` must belong to the project.
- `milestoneId`, `researchDecisionId`, and `experimentId` (if provided) must belong to `{projectId}`. Cross-project links are rejected with `400 Bad Request`.
- Initial task creation does not record a history entry; TaskHistory records every subsequent actual status transition (e.g. `TODO -> IN_PROGRESS`, `IN_PROGRESS -> COMPLETED`).

### 2.2 List Project Tasks

`GET /api/projects/{projectId}/tasks`

Returns all tasks strictly scoped to `{projectId}`.

**Optional Query Parameters:**
- `status`: Filter by `TODO`, `IN_PROGRESS`, or `COMPLETED`.
- `assignedToUserId`: Filter by primary assignee.
- `milestoneId`: Filter by milestone.

### 2.3 Get One Task

`GET /api/tasks/{taskId}`

Returns a single task by its unique ID. Returns `404 Not Found` if the task does not exist.

### 2.4 Update Task Details

`PUT /api/tasks/{taskId}`

Modifies general task fields (`title`, `description`, `dueDate`, `milestoneId`, `assignedToUserId`, `researchDecisionId`, `experimentId`). Validates that referenced milestone, research decision, and experiment belong to the task's project.

### 2.5 Update Task Status (Lifecycle Transition)

`PUT /api/tasks/{taskId}/status`

Updates the task status.

**Request Body:**
```json
{
  "status": "COMPLETED",
  "changedByUserId": 7
}
```

**Behavior:**
- If the new status differs from the current status:
  - Updates `task.status` and `task.updatedAt`.
  - Appends an entry to `task_history` within the same transaction.
  - If the task is linked to a `researchDecisionId` or `experimentId` and transitions to `COMPLETED`, a `ResearchEvent` of type `TASK_COMPLETED` is inserted into the timeline within the same transaction.
- If the new status matches the current status:
  - Idempotent: no new history record or timeline event is generated.
- If any database operation fails, the entire transaction is rolled back.

### 2.6 Get Task Lifecycle History

`GET /api/tasks/{taskId}/history`

Returns the chronological history of status changes for `{taskId}`.

**Response Body:**
```json
[
  {
    "id": 105,
    "taskId": 10,
    "previousStatus": "TODO",
    "newStatus": "IN_PROGRESS",
    "changedByUserId": 7,
    "changedAt": "2026-10-31T11:30:00Z"
  },
  {
    "id": 112,
    "taskId": 10,
    "previousStatus": "IN_PROGRESS",
    "newStatus": "COMPLETED",
    "changedByUserId": 7,
    "changedAt": "2026-10-31T15:00:00Z"
  }
]
```

Returns `404 Not Found` if `{taskId}` does not exist.

### 2.7 Find Tasks by Research Artifacts

- `GET /api/tasks/research-decision/{decisionId}`: Returns all tasks linked to the specified research decision.
- `GET /api/tasks/experiment/{experimentId}`: Returns all tasks linked to the specified experiment.
- `GET /api/projects/{projectId}/decisions/{decisionId}/tasks`: Returns all tasks linked to the specified research decision within a specific project.

### 2.8 Accept Task Suggestion with Research Links

`POST /api/task-suggestions/{suggestionId}/accept`

Converts an AI-generated task suggestion into a concrete `Task`.

**Request Body:**
```json
{
  "projectId": 1,
  "milestoneId": 12,
  "assignedToUserId": 7,
  "researchDecisionId": 4,
  "experimentId": 2,
  "title": "Cleaned up task title",
  "description": "Reviewed instructions",
  "dueDate": "2026-11-20"
}
```

The created task inherits `meetingId` from the suggestion's originating extraction review and stores `taskSuggestionId = suggestionId`.

---

## 3. Progress Derivation Rules

The progress engine computes deterministic, non-stored progress values derived directly from task completion states.

### 3.1 Student Progress

`GET /api/students/{studentId}/progress` (or project-scoped `GET /api/projects/{projectId}/students/{studentId}/progress`)

```text
student progress = (completed assigned tasks / total assigned tasks) × 100
```

- Only tasks with `status == COMPLETED` count as completed.
- Tasks assigned to other students are strictly excluded.
- If the student has 0 assigned tasks, progress is `0.00`.
- Computed using `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`.

### 3.2 Milestone Progress

Derived dynamically for each milestone:

```text
milestone progress = (completed tasks in milestone / total tasks in milestone) × 100
```

- All queries are scoped by both `milestoneId` AND `projectId` to prevent cross-project pollution.
- If a milestone has 0 tasks, its progress is `0.00`.

### 3.3 Project Progress Formula

`GET /api/projects/{projectId}/progress`

Project progress incorporates weighted milestones and milestone-less tasks:

```text
project progress =
  sum(milestone weight × milestone progress) + (milestoneless task progress × milestoneless weight)
  -------------------------------------------------------------------------------------------------
                       sum(milestone weights) + milestoneless weight
```

**Key Invariants:**
1. **Milestones with positive weights**: Each positive-weighted milestone contributes according to its derived task completion percentage. Milestones with zero or negative weights are ignored.
2. **Milestone-less Tasks**: Tasks belonging to the project that are not attached to any milestone (`milestoneId IS NULL`) are aggregated into a synthetic milestone category so their completion contributes to overall project velocity.
3. **Cross-Project Isolation**: All repository queries strictly filter by `projectId`.
4. **Boundary Guarantees**:
   - If a project has zero tasks and zero milestones, progress is `0.00`.
   - Resulting progress is always clamped between `0.00` and `100.00`.
   - Never computed as the simple average of individual student percentages.
