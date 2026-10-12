export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'

export type ProjectMemberRole = 'PROFESSOR' | 'STUDENT'

export type MilestoneStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'BLOCKED'
  | 'DEFERRED'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED'

export type TaskSuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type Project = {
  id: number | null
  name: string | null
  description: string | null
  status: ProjectStatus | null
}

export type ProjectMember = {
  id: number | null
  projectId: number | null
  userId: number | null
  role: ProjectMemberRole | null
}

export type StudentProgressItem = {
  studentId: number | null
  progress: number | null
}

export type Milestone = {
  id: number | null
  projectId: number | null
  title: string | null
  description: string | null
  status: MilestoneStatus | null
  dueDate: string | null
  weight: number | null
}

export type MilestoneProgressItem = {
  milestoneId: number | null
  progress: number | null
}

export type Meeting = {
  id: number | null
  projectId: number | null
  title: string | null
  meetingDate: string | null
  notes: string | null
}

export type Task = {
  id: number | null
  milestoneId: number | null
  assigneeId: number | null
  title: string | null
  description: string | null
  status: TaskStatus | null
  dueDate: string | null
}

export type TaskSuggestion = {
  id: number | null
  meetingId: number | null
  projectId: number | null
  title: string | null
  description: string | null
  assigneeName: string | null
  assigneeId: number | null
  milestoneId: number | null
  dueDate: string | null
  sourceExcerpt: string | null
  confidence: number | null
  status: TaskSuggestionStatus | null
  acceptedTaskId: number | null
}

export type SuggestionUpdate = {
  title: string | null
  description: string | null
  assigneeId: number | null
  milestoneId: number | null
  dueDate: string | null
}

export type ProjectDashboard = {
  project: Project | null
  projectProgress: number | null
  members: ProjectMember[]
  studentProgress: StudentProgressItem[]
  milestones: Milestone[]
  milestoneProgress: MilestoneProgressItem[]
  meetings: Meeting[]
  tasks: Task[]
  pendingTaskSuggestions: TaskSuggestion[]
}
