# Task and Progress API Contract

This document defines the initial API contract for the **Project Management & Progress** module. It is designed to integrate with the existing project, user, and milestone resources.

## Scope

The module owns:

- Task creation and management
- Primary student assignment
- Task status transitions
- Derived milestone progress
- Weighted project progress calculation

Student progress is owned by this module but is not implemented yet.

Research decisions, experiments, results, and research-event traceability are owned by the Research Traceability module.

## Task model

A task represents one unit of work in a project. For the MVP, every task has exactly one primary student assignee.

| Field | Type | Required | Description |
|---|---|---:|---|
| `id` | UUID/Long | generated | Unique task identifier |
| `projectId` | UUID/Long | yes | Project containing the task |
| `milestoneId` | UUID/Long | no | Shared milestone contributed to by the task |
| `assignedToUserId` | UUID/Long | yes | Primary student responsible for the task |
| `title` | string | yes | Short task title |
| `description` | string | no | Detailed work description |
| `status` | enum | yes | `TODO`, `IN_PROGRESS`, or `COMPLETED` |
| `dueDate` | date | no | Expected completion date |
| `createdAt` | datetime | generated | Creation timestamp |
| `updatedAt` | datetime | generated | Last modification timestamp |

## Task endpoints

### Create a task

`POST /api/projects/{projectId}/tasks`

Example request:

```json
{
  "milestoneId": 12,
  "assignedToUserId": 7,
  "title": "Evaluate Qwen-7B on the baseline dataset",
  "description": "Run the baseline evaluation and record the metrics.",
  "status": "TODO",
  "dueDate": "2026-09-25"
}
```

The service must verify that the milestone and assignee belong to the selected project.

### List project tasks

`GET /api/projects/{projectId}/tasks`

Optional filters:

- `status`
- `assignedToUserId`
- `milestoneId`

### Get one task

`GET /api/tasks/{taskId}`

### Update a task

`PUT /api/tasks/{taskId}`

The update operation may change the title, description, milestone, assignee, due date, or status. `TaskStatus.COMPLETED` is the completed state.

### Delete a task

`DELETE /api/tasks/{taskId}`

Deletion should be rejected or carefully handled if the task is referenced by an accepted audit or progress record in a later version. For the MVP, the API may permit deletion while recording normal audit timestamps.

## Progress rules

The current progress API is:

`GET /api/projects/{projectId}/progress`

`TaskStatus.COMPLETED` is the completed state. `TODO` and `IN_PROGRESS` do not count as completed.

Milestone progress is derived from tasks at calculation time. It is not stored on `Milestone`.

Student progress is not implemented yet. Project progress is not an average of student percentages.

### Milestone progress

Milestone progress is calculated from the tasks linked to that milestone:

```text
milestone progress = completed tasks / total tasks × 100
```

Only tasks with status `COMPLETED` count as completed. `TODO` and `IN_PROGRESS` do not.

If a milestone has no tasks, its progress is `0`. Values are calculated with `BigDecimal` and rounded to 2 decimal places using `HALF_UP`.

### Project progress

Project progress is a weighted average of that derived milestone progress. Only milestones with a positive weight contribute. Null, zero, and negative weights are ignored.

```text
project progress =
  sum(milestone weight × milestone progress) / sum(positive milestone weights)
```

Milestone progress and project progress both use percentage values from `0` to `100`. If a project has no valid weighted milestones, project progress is `0.00`.

## Example

A project contains two positively weighted milestones:

| Milestone | Weight | Tasks | Derived progress |
|---|---:|---|---:|
| Baseline evaluation | 40 | 1 of 1 `COMPLETED` | 100.00 |
| Error analysis | 60 | 1 of 2 `COMPLETED` | 50.00 |

Therefore:

```text
project progress = (40 × 100 + 60 × 50) / (40 + 60)
                 = 70.00
```

Two students may contribute separate tasks to the same milestone. Those tasks are combined when deriving that milestone's progress, which then affects weighted project progress.

## Validation rules

- A task title must not be blank.
- A task must belong to an existing project.
- The primary assignee must be a student who belongs to the project.
- A selected milestone must belong to the same project as the task.
- A milestone weight must be positive to contribute to project progress. Null, zero, and negative weights are ignored.
- Progress values remain within `0` to `100` percent.
- The project progress calculation must not use the average of student progress values.

## Initial test cases

1. Create a task with a valid project, milestone, and student.
2. Reject a task whose assignee belongs to another project.
3. Change a task from `TODO` to `COMPLETED`.
4. Student progress is not implemented yet.
5. Derive milestone progress when tasks are assigned to two different students.
6. Calculate weighted project progress from two milestones.
7. Return `0.00` project progress when there are no valid weighted milestones, and `0` milestone progress when a milestone has no tasks.
8. Reject a task linked to a milestone from another project.
