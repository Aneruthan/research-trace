import assert from 'node:assert/strict'
import test from 'node:test'
import { formatDateOrTimestamp, localIsoDate } from './dates.ts'

test('localIsoDate uses the local calendar day, not the UTC day', () => {
  assert.equal(localIsoDate(new Date(2026, 10, 16, 0, 30)), '2026-11-16')
  assert.equal(localIsoDate(new Date(2026, 0, 5, 23, 59)), '2026-01-05')
})

test('formatDateOrTimestamp leaves date-only values unchanged', () => {
  assert.equal(formatDateOrTimestamp('2026-10-07'), '2026-10-07')
  assert.equal(formatDateOrTimestamp(' 2026-10-07 '), '2026-10-07')
})

test('formatDateOrTimestamp formats timestamps and passes through other values', () => {
  const timestamp = '2026-10-07T14:30:00'
  assert.equal(formatDateOrTimestamp(timestamp), new Date(timestamp).toLocaleString())
  assert.equal(formatDateOrTimestamp('not a date'), 'not a date')
  assert.equal(formatDateOrTimestamp(null), null)
  assert.equal(formatDateOrTimestamp('  '), null)
})
