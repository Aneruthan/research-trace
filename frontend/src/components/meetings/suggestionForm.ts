export type StudentOption = {
  id: number
  name: string
}

export type AssigneeChoice = {
  value: string
  label: string
}

export function assigneeChoices(
  students: StudentOption[],
  currentAssigneeId: number | null,
): AssigneeChoice[] {
  const choices: AssigneeChoice[] = [{ value: '', label: 'Unassigned' }]
  for (const student of students) {
    const name = student.name.trim()
    choices.push({ value: String(student.id), label: name === '' ? 'Unknown assignee' : name })
  }
  if (currentAssigneeId != null && !students.some((student) => student.id === currentAssigneeId)) {
    choices.push({ value: String(currentAssigneeId), label: 'Unknown assignee' })
  }
  return choices
}
