import { describe, expect, it } from 'vitest'
import { ApiRequestError } from './api/http'
import { describeImportError, importFailureFeedback } from './importFeedback'

describe('import feedback', () => {
  it('formats the first validation issue with its row, field, value and remedy', () => {
    expect(describeImportError({ rowNumber: 2, fieldName: '费别', originalValue: '自费病人', message: '费别未配置启用系数' }))
      .toBe('第 2 行「费别」“自费病人”：费别未配置启用系数')
  })

  it('keeps the batch number and count of other errors for navigation to details', () => {
    const feedback = importFailureFeedback(new ApiRequestError('导入校验失败', {
      batchNo: 'ARR-FAILED-001',
      errors: [
        { rowNumber: 2, fieldName: '费别', originalValue: '自费病人', message: '费别未配置启用系数' },
        { rowNumber: 3, fieldName: '住院病区', originalValue: '康复病区', message: '科室无法匹配' },
      ],
    }), '导入失败')

    expect(feedback.message).toContain('第 2 行「费别」“自费病人”：费别未配置启用系数')
    expect(feedback.batchNo).toBe('ARR-FAILED-001')
    expect(feedback.remainingErrors).toBe(1)
  })
})
