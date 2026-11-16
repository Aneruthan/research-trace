import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { assigneeLabel, studentLabel } from '../../api/users'
import { formatDateOrTimestamp, localIsoDate } from '../common/dates'
import { isOverdueTask, taskCounts } from '../../work/work'
import type {
  Meeting,
  Milestone,
  ProjectDashboard,
  StudentProgressItem,
  Task,
  TaskSuggestion,
  User,
} from '../../api/types'

type DashboardViewProps = {
  dashboard: ProjectDashboard
  users: User[]
}

export function DashboardView({ dashboard, users }: DashboardViewProps) {
  const project = dashboard.project
  const progressByMilestone = new Map(
    dashboard.milestoneProgress.map((item) => [item.milestoneId, item.progress]),
  )
  const milestoneTitleMap = new Map<number, string>(
    dashboard.milestones
      .filter((m) => m.id != null && m.title != null)
      .map((m) => [m.id as number, m.title as string]),
  )
  const meetings = [...dashboard.meetings].sort((left, right) =>
    (right.meetingDate ?? '').localeCompare(left.meetingDate ?? ''),
  )

  return (
    <div className="mt-6 space-y-6">
      <section className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-stone-950">
              {project?.name ?? 'Untitled project'}
            </h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">
              {project?.description ?? 'No description'}
            </p>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-stone-600">
              Objectives: {project?.objectives ?? 'None recorded'}
            </p>
            <p className="mt-2 text-xs text-stone-500">
              {formatDateOrTimestamp(project?.startDate ?? null) ?? 'No start date'}
              {' – '}
              {formatDateOrTimestamp(project?.targetDate ?? null) ?? 'No target date'}
            </p>
            <p className="mt-1 text-xs text-stone-500">
              Supervisor: {project?.supervisorId == null
                ? 'Unassigned'
                : users.find((user) => user.id === project.supervisorId)?.name?.trim() || 'Unknown supervisor'}
            </p>
          </div>
          <span className="w-fit rounded-full bg-stone-950 px-3 py-1 text-xs font-medium tracking-wide text-stone-50">
            {project?.status ?? 'No status'}
          </span>
        </div>
        <ProgressMeter label="Project progress" value={dashboard.projectProgress} />
        <p className="mt-3 text-xs text-stone-600">
          {taskCounts(dashboard.tasks, localIsoDate()).overdue} overdue tasks in this project.
          Overdue means the due date is before today and the task is not completed.
        </p>
      </section>

      {dashboard.researchMetrics ? (
        <section className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-sm font-semibold tracking-tight text-stone-950">Research Traceability</h2>
              <p className="mt-1 text-xs text-stone-500">Decisions, experiments, evidence, and detected direction changes.</p>
            </div>
            <Link
              to="/research-trace"
              className="text-xs font-medium text-stone-900 underline hover:text-stone-700"
            >
              View Research Trace &rarr;
            </Link>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
            <MetricCard
              label="Decisions"
              value={dashboard.researchMetrics.totalDecisions}
              subtext={`${dashboard.researchMetrics.activeDecisions} active`}
            />
            <MetricCard label="Active Decisions" value={dashboard.researchMetrics.activeDecisions} />
            <MetricCard label="Experiments" value={dashboard.researchMetrics.totalExperiments} />
            <MetricCard label="Results" value={dashboard.researchMetrics.totalResults} />
            <MetricCard label="Timeline Events" value={dashboard.researchMetrics.totalEvents} />
            <MetricCard label="Changes" value={dashboard.researchMetrics.totalChanges} />
          </div>
        </section>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-2">
        <Panel title="Student progress">
          {dashboard.studentProgress.length === 0 ? (
            <Empty>No student progress for this project.</Empty>
          ) : (
            <ul className="space-y-4">
              {dashboard.studentProgress.map((item) => (
                <StudentProgressRow key={item.studentId ?? 'unknown'} item={item} users={users} tasks={dashboard.tasks} />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Milestones">
          {dashboard.milestones.length === 0 ? (
            <Empty>No milestones for this project.</Empty>
          ) : (
            <ul className="space-y-4">
              {dashboard.milestones.map((milestone) => (
                <MilestoneRow
                  key={milestone.id ?? milestone.title}
                  milestone={milestone}
                  progress={progressByMilestone.get(milestone.id) ?? null}
                />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Meetings">
          {meetings.length === 0 ? (
            <Empty>No meetings for this project.</Empty>
          ) : (
            <ul className="divide-y divide-stone-100">
              {meetings.map((meeting) => (
                <MeetingRow key={meeting.id ?? meeting.title} meeting={meeting} />
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Tasks">
          {dashboard.tasks.length === 0 ? (
            <Empty>No tasks for this project.</Empty>
          ) : (
            <ul className="divide-y divide-stone-100">
              {dashboard.tasks.map((task) => (
                <TaskRow
                  key={task.id ?? task.title}
                  task={task}
                  users={users}
                  milestoneTitleMap={milestoneTitleMap}
                />
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <Panel title="Pending task suggestions">
        {dashboard.pendingTaskSuggestions.length === 0 ? (
          <Empty>No pending suggestions.</Empty>
        ) : (
          <ul className="divide-y divide-stone-100">
            {dashboard.pendingTaskSuggestions.map((suggestion) => (
              <SuggestionRow key={suggestion.id ?? suggestion.title} suggestion={suggestion} users={users} />
            ))}
          </ul>
        )}
      </Panel>
    </div>
  )
}

function MetricCard({ label, value, subtext }: { label: string; value: number; subtext?: string }) {
  return (
    <div className="rounded border border-stone-100 bg-stone-50 p-3">
      <p className="text-xs font-medium text-stone-500">{label}</p>
      <p className="mt-1 text-xl font-bold tracking-tight text-stone-900">{value}</p>
      {subtext ? <p className="mt-0.5 text-[11px] text-stone-500">{subtext}</p> : null}
    </div>
  )
}

function ProgressMeter({ label, value }: { label: string; value: number | null }) {
  const width = value == null ? 0 : Math.min(100, Math.max(0, value))
  return (
    <div className="mt-5">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-sm font-medium text-stone-700">{label}</p>
        <p className="text-sm font-semibold text-stone-950">{formatProgress(value)}</p>
      </div>
      <div className="mt-2 h-2 overflow-hidden rounded-full bg-stone-200">
        <div className="h-full rounded-full bg-stone-950" style={{ width: `${width}%` }} />
      </div>
    </div>
  )
}

function StudentProgressRow({
  item,
  users,
  tasks,
}: {
  item: StudentProgressItem
  users: User[]
  tasks: Task[]
}) {
  const counts = taskCounts(tasks.filter((task) => task.assigneeId === item.studentId), localIsoDate())
  return (
    <li>
      <p className="text-sm font-medium text-stone-900">{studentLabel(item.studentId, users)}</p>
      <p className="text-xs text-stone-500">
        {counts.completed} completed · {counts.inProgress} in progress · {counts.pending} pending · {counts.overdue} overdue
      </p>
      <ProgressMeter label="Progress" value={item.progress} />
    </li>
  )
}

function MilestoneRow({
  milestone,
  progress,
}: {
  milestone: Milestone
  progress: number | null | undefined
}) {
  return (
    <li>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-medium text-stone-900">{milestone.title ?? 'Untitled milestone'}</p>
          {milestone.weight != null ? (
            <p className="text-[11px] text-stone-500">Weight: {milestone.weight} (not used in progress)</p>
          ) : null}
        </div>
        <span className="text-xs font-medium tracking-wide text-stone-500">
          {milestone.status ?? 'No status'}
        </span>
      </div>
      <ProgressMeter label="Progress" value={progress ?? null} />
    </li>
  )
}

function MeetingRow({ meeting }: { meeting: Meeting }) {
  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <p className="text-sm font-medium text-stone-900">{meeting.title ?? 'Untitled meeting'}</p>
      <p className="text-xs text-stone-500">{meeting.meetingDate ?? 'No date'}</p>
    </li>
  )
}

function TaskRow({
  task,
  users,
  milestoneTitleMap,
}: {
  task: Task
  users: User[]
  milestoneTitleMap: Map<number, string>
}) {
  const milestoneName = task.milestoneId != null
    ? milestoneTitleMap.get(task.milestoneId) ?? `Milestone #${task.milestoneId}`
    : 'No milestone'

  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <div>
        <p className="text-sm font-medium text-stone-900">{task.title ?? 'Untitled task'}</p>
        <p className="text-xs text-stone-500">
          Assignee: {assigneeLabel(task.assigneeId, users)} · Milestone: {milestoneName}
          {task.dueDate ? ` · Due ${task.dueDate}` : ''}
          {isOverdueTask(task, localIsoDate()) ? ' · Overdue' : ''}
        </p>
        {(task.researchDecisionId || task.experimentId) ? (
          <div className="mt-1 flex flex-wrap gap-2">
            {task.researchDecisionId ? (
              <span className="inline-flex items-center rounded bg-stone-100 px-1.5 py-0.5 text-[11px] font-medium text-stone-700">
                Decision #{task.researchDecisionId}
              </span>
            ) : null}
            {task.experimentId ? (
              <span className="inline-flex items-center rounded bg-stone-100 px-1.5 py-0.5 text-[11px] font-medium text-stone-700">
                Experiment #{task.experimentId}
              </span>
            ) : null}
          </div>
        ) : null}
      </div>
      <span className="text-xs font-medium tracking-wide text-stone-500">{task.status ?? 'No status'}</span>
    </li>
  )
}

function SuggestionRow({ suggestion, users }: { suggestion: TaskSuggestion; users: User[] }) {
  return (
    <li className="flex flex-col gap-1 py-3 first:pt-0 last:pb-0 sm:flex-row sm:items-baseline sm:justify-between">
      <div>
        <p className="text-sm font-medium text-stone-900">{suggestion.title ?? 'Untitled suggestion'}</p>
        <p className="text-xs text-stone-500">
          {assigneeLabel(suggestion.assigneeId, users, suggestion.assigneeName)}
        </p>
        {suggestion.sourceExcerpt ? (
          <p className="mt-1 text-xs italic text-stone-600 line-clamp-2">
            "{suggestion.sourceExcerpt}"
          </p>
        ) : null}
      </div>
      <span className="text-xs font-medium tracking-wide text-stone-500">{suggestion.status ?? 'PENDING'}</span>
    </li>
  )
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <h2 className="text-sm font-semibold tracking-tight text-stone-950">{title}</h2>
      <div className="mt-4">{children}</div>
    </section>
  )
}

function Empty({ children }: { children: ReactNode }) {
  return <p className="text-sm text-stone-500">{children}</p>
}

function formatProgress(value: number | null): string {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  return `${value.toFixed(2)}%`
}
