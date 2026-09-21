# ResearchTrace Data Model

ResearchTrace keeps three concerns distinct:

- **Student progress:** work completed by one student.
- **Project progress:** weighted progress across shared project milestones.
- **Research trace:** a chronological record of research questions, hypotheses, meetings, decisions, experiments, results, and observations.

All entities can use standard audit fields such as `id`, `createdAt`, and `updatedAt`.

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

**Purpose:** Represents one unit of work and is the main input for student progress.

**Important fields:** `id`, `projectId`, `milestoneId` (optional), `assignedToUserId`, `title`, `description`, `status` (`TODO`, `IN_PROGRESS`, `COMPLETED`), `dueDate`, `completedAt`. `TaskStatus.COMPLETED` is the completed state; `TODO` and `IN_PROGRESS` do not count as completed.

**Relationships:** Each task belongs to one project, has exactly one primary student assignee for the MVP, and may contribute to one shared milestone. A milestone can receive contributions from many tasks, including tasks assigned to different students.

## Meeting

**Purpose:** Stores a project meeting and its notes or AI-extracted summary.

**Important fields:** `id`, `projectId`, `title`, `heldAt`, `notes`, `summary`, `createdByUserId`, `sourceReference`.

**Relationships:** Each meeting belongs to one project and can lead to many research decisions. A related `ResearchEvent` records the meeting in the chronological trace.

## ResearchDecision

**Purpose:** Records a research choice and the reasoning behind it.

**Important fields:** `id`, `projectId`, `meetingId` (optional), `title`, `decisionText`, `rationale`, `status` (`ACTIVE` or `SUPERSEDED`), `supersedesDecisionId` (optional), `decidedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each decision belongs to one project and may originate from one meeting. One decision can guide many experiments. If a later decision changes direction, it is saved as a new decision whose `supersedesDecisionId` points to the earlier decision; the earlier decision remains stored with status `SUPERSEDED`. A related `ResearchEvent` adds the decision to the trace.

## Experiment

**Purpose:** Describes a test or study used to investigate a question or validate a decision.

**Important fields:** `id`, `projectId`, `researchDecisionId` (optional for exploratory work), `title`, `hypothesis`, `method`, `status` (`PLANNED`, `RUNNING`, `COMPLETED`), `startedAt`, `completedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each experiment belongs to one project and may be motivated by one research decision. A decision can guide many experiments, and an experiment can have many results. Important experiment changes are recorded as research events.

## Result

**Purpose:** Captures an outcome, metric, observation, or conclusion from an experiment.

**Important fields:** `id`, `projectId`, `experimentId`, `summary`, `dataOrMetric`, `observedAt`, `createdByUserId`, `sourceReference`.

**Relationships:** Each result belongs to one project and one experiment; an experiment can have many results. Results can provide evidence for later decisions. A related research event places the result in the trace.

## ResearchEvent

**Purpose:** Provides the chronological research-trace timeline without duplicating detailed records.

**Important fields:** `id`, `projectId`, `eventType` (`MEETING`, `DECISION`, `EXPERIMENT`, `RESULT`, `OBSERVATION`, `HYPOTHESIS`, or `RESEARCH_QUESTION`), `occurredAt`, `title`, `description`, `createdByUserId`, `meetingId`, `researchDecisionId`, `experimentId`, `resultId`, `sourceReference`.

**Relationships:** Each event belongs to one project. Meeting, decision, experiment, and result events link to their respective detailed entity and contain only concise timeline information. Observation, hypothesis, and research-question events are recorded directly when a separate MVP entity is unnecessary. Events are ordered by `occurredAt` (then `createdAt`) to form the research trace.

## Important Relationships and Rules

### Student progress and project progress

Student progress is not implemented yet. It remains a distinct concern: work completed by one student, not an input to project progress.

Project progress is not an aggregation of student percentages. Tasks contribute to a shared milestone through their status. Milestone progress is derived, not stored:

`milestone progress = completed tasks / total tasks × 100`

Only `COMPLETED` tasks count as completed. If a milestone has no tasks, its progress is `0`. Project progress is then the weighted average of those derived milestone percentages:

`project progress = sum(milestone weight × milestone progress) / sum(positive milestone weights)`

Only positive milestone weights contribute. Null, zero, and negative weights are ignored. If a project has no valid weighted milestones, project progress is `0`.

### Shared project example

In one project, Aneruthan and Gayathri are both linked through `ProjectMember`. A shared milestone, **Experiment Evaluation**, has weight 30. Aneruthan's task is to implement evaluation metrics, while Gayathri's task is to prepare the evaluation dataset. Each task has one primary assignee, but both use the same `milestoneId`. If Aneruthan has `COMPLETED` their task and Gayathri's task is still `TODO` or `IN_PROGRESS`, the milestone progress is `50`. That derived progress is then weighted by 30 in the project average.

### Provenance and decision history

For the MVP, `sourceReference` is a simple text field. It may hold a URL, transcript filename, notebook path, dataset version, report section, or other identifier. No separate source or attachment entity is required. Prior decisions are never deleted when the team changes direction; the newer decision supersedes the earlier record, retaining the full decision history.

### Chronological research trace

Create a `ResearchEvent` for important research activity. A typical chain is: research question → hypothesis → meeting → decision → experiment → result → revised decision. The event links make the supporting records easy to inspect while the timeline stays concise.
