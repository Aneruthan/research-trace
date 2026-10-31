# ResearchTrace Data Model

ResearchTrace keeps three concerns distinct:

- **Student progress:** work completed by one student.
- **Project progress:** weighted progress across shared project milestones and milestone-less tasks.
- **Research trace:** a chronological record of research questions, hypotheses, meetings, decisions, experiments, results, and research-linked task completions.

All entities use standard audit fields such as `id`, `createdAt`, and `updatedAt`.

## User

**Purpose:** Represents a person using the system, such as a student or professor.

**Important fields:** `id`, `name`, `email` (unique), `role` (`STUDENT` or `PROFESSOR`).

**Relationships:** A user can belong to multiple projects through `ProjectMember`. A student can be the primary assignee of many tasks. A user can create meetings, decisions, experiments, results, and research events.

## Project

**Purpose:** Represents one research project carried out by a group.

**Important fields:** `id`, `name`, `description`, `status` (`ACTIVE`, `COMPLETED`, or `ARCHIVED`), `createdByUserId`.

**Relationships:** A project has many members, milestones, tasks, meetings, research decisions, experiments, results, and research events.

## ProjectMember

**Purpose:** Links a user to a project and records that member's role in the project.

**Important fields:** `id`, `projectId`, `userId`, `projectRole` (`STUDENT`, `PROFESSOR`, or `OWNER`), `joinedAt`.

**Relationships:** Each record belongs to one project and one user. This creates a many-to-many relationship: a project can have multiple students and a student can join multiple projects. There should be only one record for each `(projectId, userId)` pair.

## Milestone

**Purpose:** Represents a shared, measurable project goal, such as completing a literature review or submitting an experiment report.

**Important fields:** `id`, `projectId`, `title`, `description`, `dueDate`, `weight`, `status` (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`). Milestone progress is derived from tasks and is not stored on the milestone.

**Relationships:** Each milestone belongs to one project and can have many tasks. Tasks assigned to different students can contribute to the same milestone. A positive milestone weight determines that milestone's contribution to project progress. Null, zero, and negative weights are ignored.

## Task

**Purpose:** Represents one unit of work and is the main input for student progress and milestone progress.

**Important fields:** `id`, `projectId`, `milestoneId` (optional), `assignedToUserId`, `meetingId` (optional provenance), `taskSuggestionId` (optional suggestion provenance), `researchDecisionId` (optional research link), `experimentId` (optional experiment link), `title`, `description`, `status` (`TODO`, `IN_PROGRESS`, `COMPLETED`), `dueDate`, `createdAt`, `updatedAt`. `TaskStatus.COMPLETED` is the completed state; `TODO` and `IN_PROGRESS` do not count as completed.

**Relationships:** Each task belongs to one project, has exactly one primary student assignee, and may contribute to one shared milestone. A task may originate from a supervision `Meeting` or AI `TaskSuggestion`. A task may link to a motivating `ResearchDecision` and/or `Experiment`. Modifying task status records an entry in `TaskHistory`, and completing a research-linked task emits a `TASK_COMPLETED` research event.

## TaskHistory

**Purpose:** Records an immutable audit log of task status transitions.

**Important fields:** `id`, `taskId`, `previousStatus` (nullable for initial creation), `newStatus`, `changedByUserId` (nullable), `changedAt`.

**Relationships:** Belongs to one `Task`. Populated automatically within the same database transaction whenever a task's status changes.

## TaskSuggestion

**Purpose:** Holds an AI-extracted action item from a meeting transcript before human review.

**Important fields:** `id`, `extractionReviewId`, `title`, `description`, `suggestedAssigneeId` (optional), `milestoneId` (optional), `researchDecisionId` (optional), `experimentId` (optional), `status` (`PENDING`, `ACCEPTED`, `REJECTED`).

**Relationships:** Belongs to one `ExtractionReview`. When accepted by a reviewer, instantiates a concrete `Task` preserving provenance and links.

## Meeting

**Purpose:** Stores a project meeting and its notes or AI-extracted summary.

**Important fields:** `id`, `projectId`, `title`, `heldAt`, `notes`, `summary`, `createdByUserId`, `sourceReference`.

**Relationships:** Each meeting belongs to one project and can lead to many research decisions, experiments, results, and tasks. A related `ResearchEvent` records the meeting in the chronological trace.

## ResearchDecision

**Purpose:** Records a research choice and the reasoning behind it.

**Important fields:** `id`, `projectId`, `meetingId` (optional), `title`, `decisionText`, `rationale`, `status` (`ACTIVE` or `SUPERSEDED`), `supersedesDecisionId` (optional), `decidedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each decision belongs to one project and may originate from one meeting. One decision can guide many experiments and tasks. If a later decision changes direction, it is saved as a new decision whose `supersedesDecisionId` points to the earlier decision; the earlier decision remains stored with status `SUPERSEDED`. A related `ResearchEvent` adds the decision to the trace.

## Experiment

**Purpose:** Describes a test or study used to investigate a question or validate a decision.

**Important fields:** `id`, `projectId`, `meetingId` (optional provenance), `researchDecisionId` (optional for exploratory work), `title`, `hypothesis`, `method`, `status` (`PLANNED`, `RUNNING`, `COMPLETED`), `startedAt`, `completedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each experiment belongs to one project and may be motivated by one research decision and originate from one meeting. A decision can guide many experiments, an experiment can have many results, and tasks can be linked directly to an experiment. Important experiment changes are recorded as research events.

## Result

**Purpose:** Captures an outcome, metric, observation, or conclusion from an experiment.

**Important fields:** `id`, `projectId`, `experimentId`, `meetingId` (optional provenance), `summary`, `dataOrMetric`, `observedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each result belongs to one project and one experiment; an experiment can have many results. Results can originate from a meeting and provide evidence for later decisions. A related research event places the result in the trace.

## ResearchEvent

**Purpose:** Provides the chronological research-trace timeline without duplicating detailed records.

**Important fields:** `id`, `projectId`, `eventType` (`MEETING`, `DECISION`, `EXPERIMENT`, `RESULT`, `OBSERVATION`, `HYPOTHESIS`, `RESEARCH_QUESTION`, `TASK_COMPLETED`), `occurredAt`, `title`, `description`, `createdByUserId`, `meetingId`, `researchDecisionId`, `experimentId`, `resultId`, `taskId`, `sourceReference`.

**Relationships:** Each event belongs to one project. Meeting, decision, experiment, result, and task completion events link to their respective detailed entity and contain concise timeline information. Events are ordered by `occurredAt` (then `createdAt` and `id`) to form the research trace.

## Important Relationships and Rules

### Student progress and project progress

Student progress is calculated from the tasks assigned to one student: `completed assigned tasks / total assigned tasks × 100`. Only `COMPLETED` tasks count, and a student with no assigned tasks has progress `0.00`. It remains a distinct concern: work completed by one student, not an input to project progress.

Project progress is not an aggregation of student percentages. Tasks contribute to shared milestones or to the project directly if unassigned to a milestone. Milestone progress is derived dynamically, not stored:

`milestone progress = completed tasks in milestone / total tasks in milestone × 100`

Only `COMPLETED` tasks count as completed. If a milestone has no tasks, its progress is `0.00`. Project progress is then the weighted average combining derived milestone percentages and milestone-less tasks:

`project progress = [sum(milestone weight × milestone progress) + (milestoneless progress × weight)] / total weights`

Only positive milestone weights contribute. Null, zero, and negative weights are ignored. If a project has no valid weighted milestones or tasks, project progress is `0.00`.

### Provenance and decision history

For the MVP, `sourceReference` is a simple text field. It may hold a URL, transcript filename, notebook path, dataset version, report section, or other identifier. Prior decisions are never deleted when the team changes direction; the newer decision supersedes the earlier record, retaining the full decision history.

### Chronological research trace

A `ResearchEvent` is created for important research activity. A typical chain is: research question → hypothesis → meeting → decision → experiment → result → linked task completion → revised decision. The event links make the supporting records easy to inspect while the timeline stays concise.
