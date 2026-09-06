import { ApiRequestError } from './api/http'

export interface ImportResult {
  batchNo: string
  total: number
  success: number
  failure: number
  added: number
  overwritten: number
  skipped: number
}

export interface ImportError {
  rowNumber: number
  fieldName: string
  originalValue: string | null
  message: string
}

interface ImportFailure {
  batchNo?: string | null
  errors?: ImportError[]
}

export interface ImportFeedback extends ImportResult {
  status: 'success' | 'error'
  message: string
  remainingErrors: number
}

const displayValue = (value: string | null) => value?.trim() ? `“${value.trim()}”` : '空值'

export const describeImportError = (error: ImportError) =>
  `第 ${error.rowNumber} 行「${error.fieldName}」${displayValue(error.originalValue)}：${error.message}`

export const importFailureFeedback = (error: unknown, fallback: string): ImportFeedback => {
  const failure = error instanceof ApiRequestError ? error.data as ImportFailure | undefined : undefined
  const errors = Array.isArray(failure?.errors) ? failure.errors : []
  const first = errors[0]
  return {
    status: 'error',
    message: first ? `导入校验失败：${describeImportError(first)}` : error instanceof Error ? error.message : fallback,
    batchNo: failure?.batchNo || '',
    total: errors.length,
    success: 0,
    failure: errors.length,
    added: 0,
    overwritten: 0,
    skipped: 0,
    remainingErrors: Math.max(0, errors.length - 1),
  }
}
