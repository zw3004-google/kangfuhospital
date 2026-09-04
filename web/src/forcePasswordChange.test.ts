import { describe, expect, it } from 'vitest'
import { shouldRedirectToPasswordChange } from './forcePasswordChange'

describe('shouldRedirectToPasswordChange', () => {
  it('redirects authenticated users from normal pages when password change is required', () => {
    expect(shouldRedirectToPasswordChange(true, '/dashboard')).toBe(true)
  })

  it('does not reload the password change page', () => {
    expect(shouldRedirectToPasswordChange(true, '/system/change-password')).toBe(false)
  })

  it('does not redirect from login or when password change is not required', () => {
    expect(shouldRedirectToPasswordChange(true, '/login')).toBe(false)
    expect(shouldRedirectToPasswordChange(false, '/dashboard')).toBe(false)
  })
})
