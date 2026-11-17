import assert from 'node:assert/strict'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const frontendSrc = fileURLToPath(new URL('..', import.meta.url))

test('the students page lists and creates students without adding an existing one', () => {
  const source = readFileSync(new URL('./Students.tsx', import.meta.url), 'utf8')
  assert.match(source, /getProjectStudents/)
  assert.match(source, /Create student/)
  assert.match(source, /createUser/)
  assert.match(source, /to=\{`\/students\/\$\{student\.userId\}`\}/)
  assert.match(source, /Save student/)
  assert.equal(source.includes('Add an existing student'), false)
  assert.equal(source.includes('addExisting'), false)
  assert.equal(source.includes('existingUserId'), false)
  assert.equal(source.includes('Select a student'), false)
  assert.equal(source.includes('Add to project'), false)
  assert.equal(source.includes('repairStudentMembership'), false)
})

test('no frontend screen offers to add an existing student', () => {
  const offenders = sourceFiles(frontendSrc).filter((file) => {
    const source = readFileSync(file, 'utf8')
    return source.includes('Add an existing student') || source.includes('addExisting') || source.includes('existingUserId')
  })
  assert.deepEqual(offenders, [])
})

function sourceFiles(directory: string): string[] {
  return readdirSync(directory).flatMap((entry) => {
    const path = join(directory, entry)
    if (statSync(path).isDirectory()) {
      return sourceFiles(path)
    }
    return entry.endsWith('.tsx') ? [path] : []
  })
}
