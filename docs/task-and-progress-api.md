# Task and Progress API Contract

This document defines the initial API contract for the **Project Management & Progress** module. It is designed to integrate with the existing project, user, and milestone resources.

## Scope

The module owns:

- Task creation and management
- Primary student assignment
- Task status transitions
- Student progress calculation
- Milestone progress calculation
- Weighted project progress calculation

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
| `status` | enum | yes | `TODO`, `IN_PROGRESS`, or `DONE` |
| `dueDate` | date | no | Expected completion date |
| `completedAt` | datetime | no | Set when the task becomes `DONE` |
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

The update operation may change the title, description, milestone, assignee, due date, or status. A transition to `DONE` sets `completedAt`; moving away from `DONE` clears it.

### Delete a task

`DELETE /api/tasks/{taskId}`

Deletion should be rejected or carefully handled if the task is referenced by an accepted audit or progress record in a later version. For the MVP, the API may permit deletion while recording normal audit timestamps.

## Progress rules

### Student progress

Student progress is calculated only from tasks assigned to that student within the selected project:

```text
student progress = completed assigned tasks / total assigned tasks × 100
```

If the student has no assigned tasks, the API should return `0` rather than divide by zero.

### Milestone progress

For the MVP, milestone progress is calculated from the tasks linked to that milestone:

```text
milestone progress = completed milestone tasks / total milestone tasks × 100
```

If a milestone has no tasks, its progress is `0` until work is assigned.

### Project progress

Project progress is calculated from milestone weights, not from the average of student percentages:

```text
project progress =
  sum(milestone weight × milestone progress) / sum(milestone weights)
```

The implementation should normalize milestone progress to a value between `0` and `1` before applying weights, or use percentage values consistently throughout the calculation.

If a project has no weighted milestones, its progress should be `0`.

## Example

A project contains two milestones:

| Milestone | Weight | Progress |
|---|---:|---:|
| Baseline evaluation | 40 | 100% |
| Error analysis | 60 | 50% |

Therefore:

```text
project progress = (40 × 1.00 + 60 × 0.50) / (40 + 60)
                 = 70%
```

Two students may contribute separate tasks to the same milestone. Their individual task completion affects the shared milestone, which then affects the weighted project progress.

## Validation rules

- A task title must not be blank.
- A task must belong to an existing project.
- The primary assignee must be a student who belongs to the project.
- A selected milestone must belong to the same project as the task.
- A milestone weight must be positive.
- Progress values must remain within `0` to `100` percent.
- The project progress calculation must not use the average of student progress values.
- Status changes must update `completedAt` consistently.

## Initial test cases

1. Create a task with a valid project, milestone, and student.
2. Reject a task whose assignee belongs to another project.
3. Change a task from `TODO` to `DONE` and verify `completedAt` is populated.
4. Calculate student progress when the student has completed 2 of 4 tasks.
5. Calculate milestone progress when tasks are assigned to two different students.
6. Calculate weighted project progress from two milestones.
7. Return `0` progress for a student, milestone, or project with no applicable work.
8. Reject a task linked to a milestone from another project.
