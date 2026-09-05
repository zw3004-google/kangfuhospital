const PREFIX = 'h5-session-draft:'

export function saveSessionDraft(key: string, value: unknown) {
  try { sessionStorage.setItem(PREFIX + key, JSON.stringify(value)) } catch { /* storage may be disabled */ }
}

export function loadSessionDraft<T>(key: string): T | null {
  try { const value = sessionStorage.getItem(PREFIX + key); return value ? JSON.parse(value) as T : null } catch { return null }
}

export function clearSessionDraft(key: string) {
  try { sessionStorage.removeItem(PREFIX + key) } catch { /* storage may be disabled */ }
}

export function clearAllSessionDrafts() {
  try { Object.keys(sessionStorage).filter(key => key.startsWith(PREFIX)).forEach(key => sessionStorage.removeItem(key)) } catch { /* storage may be disabled */ }
}
