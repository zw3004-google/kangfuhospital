import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'

async function setup(handler: (config: any) => any) {
  vi.resetModules()
  const { http } = await import('./http')
  const calls: any[] = []
  http.defaults.adapter = (async config => {
    calls.push(config)
    const result = await handler(config)
    const response = { data: result.data, status: result.status ?? 200, statusText: '', headers: new AxiosHeaders(), config }
    if (response.status >= 400) throw new AxiosError('rejected', 'ERR_BAD_REQUEST', config, undefined, response)
    return response
  }) as AxiosAdapter
  return { http, calls }
}
const token = (value = 'one', username: string | null = 'admin') => ({ data: { data: { headerName: 'X-CSRF-TOKEN', token: value, username } } })
const failure = (code: string, status = 403) => ({ status, data: { data: { code } } })
beforeEach(() => { window.history.replaceState({}, '', '/login') })
describe('Session CSRF request pipeline', () => {
  it('shares initial token acquisition and attaches it to every write', async () => {
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token() : { data: {} })
    await Promise.all([http.post('/arrears/records'), http.put('/discharge/records/1'), http.delete('/discharge/consultations/1')])
    expect(calls.filter(c => c.url === '/auth/csrf')).toHaveLength(1)
    expect(calls.filter(c => c.url !== '/auth/csrf').every(c => c.headers.get('X-CSRF-TOKEN') === 'one')).toBe(true)
  })
  it('GET does not acquire a token', async () => {
    const { http, calls } = await setup(() => ({ data: {} }))
    await http.get('/system/me')
    expect(calls).toHaveLength(1)
    expect(calls[0].headers.get('X-CSRF-TOKEN')).toBeUndefined()
  })
  it('refreshes once for simultaneous stale-token failures', async () => {
    let n = 0
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token(++n === 1 ? 'old' : 'new')
      : c.headers.get('X-CSRF-TOKEN') === 'old' ? failure('CSRF_INVALID') : { data: {} })
    await Promise.all([http.put('/arrears/records/1'), http.put('/discharge/records/1')])
    expect(n).toBe(2)
    expect(calls.filter(c => c.url !== '/auth/csrf')).toHaveLength(4)
  })
  it('stops after one retry', async () => {
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token() : failure('CSRF_INVALID'))
    await expect(http.post('/system/users')).rejects.toThrow('页面安全凭证')
    expect(calls.filter(c => c.url === '/system/users')).toHaveLength(2)
  })
  it.each(['ACCESS_DENIED', 'ORIGIN_DENIED'])('never retries %s', async code => {
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token() : failure(code))
    await expect(http.post('/system/users')).rejects.toThrow()
    expect(calls.filter(c => c.url === '/system/users')).toHaveLength(1)
  })
  it('does not retry uncertain network failures', async () => {
    const { http, calls } = await setup(c => { if (c.url === '/auth/csrf') return token(); throw new AxiosError('timeout', 'ECONNABORTED', c) })
    await expect(http.post('/system/users')).rejects.toThrow('请求超时')
    expect(calls.filter(c => c.url === '/system/users')).toHaveLength(1)
  })
  it('does not replay a write after another account logs in', async () => {
    let n = 0
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token('t', ++n === 1 ? 'admin' : 'other') : failure('CSRF_INVALID'))
    await expect(http.post('/system/users')).rejects.toThrow('登录状态已变化')
    expect(calls.filter(c => c.url === '/system/users')).toHaveLength(1)
  })
  it('rotates the cached token on successful login and clears it on logout', async () => {
    let n = 0
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token(String(++n)) : { data: {} })
    await http.post('/auth/login', new URLSearchParams({ username: 'admin', password: 'test' }))
    await http.post('/auth/logout')
    await http.post('/auth/login')
    expect(n).toBe(4)
    expect(calls.find(c => c.url === '/auth/logout').headers.get('X-CSRF-TOKEN')).toBe('2')
  })
  it('rejects off-origin requests before token acquisition or transport', async () => {
    const { http, calls } = await setup(() => ({ data: {} }))
    await expect(http.post('https://untrusted.example/api/write')).rejects.toThrow('外部地址')
    expect(calls).toHaveLength(0)
  })
  it('does not retry an expired session', async () => {
    const { http, calls } = await setup(c => c.url === '/auth/csrf' ? token() : failure('AUTH_REQUIRED', 401))
    await expect(http.post('/system/users')).rejects.toThrow()
    expect(calls.filter(c => c.url === '/system/users')).toHaveLength(1)
  })
})
