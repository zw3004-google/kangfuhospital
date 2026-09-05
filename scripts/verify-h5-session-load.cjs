const baseUrl = process.env.H5_API_BASE_URL || 'http://127.0.0.1:8080'
const username = process.env.H5_E2E_USER
const password = process.env.H5_E2E_PASSWORD
const requests = Number(process.env.H5_LOAD_REQUESTS || 100)
if (!username || !password) throw new Error('H5_E2E_USER and H5_E2E_PASSWORD are required')

const cookies = new Map()
function updateCookies(response) {
  for (const value of response.headers.getSetCookie()) {
    const pair = value.split(';', 1)[0]
    const separator = pair.indexOf('=')
    if (separator > 0) cookies.set(pair.slice(0, separator), pair.slice(separator + 1))
  }
}
function cookieHeader() {
  return [...cookies].map(([name, value]) => `${name}=${value}`).join('; ')
}
async function call(path, init = {}) {
  const headers = new Headers(init.headers)
  if (cookies.size) headers.set('Cookie', cookieHeader())
  const response = await fetch(baseUrl + path, { ...init, headers })
  updateCookies(response)
  return response
}

;(async () => {
  let response = await call('/api/auth/csrf')
  if (!response.ok) throw new Error(`CSRF bootstrap failed: ${response.status}`)
  let token = (await response.json()).data
  response = await call('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Origin: baseUrl,
      [token.headerName]: token.token,
    },
    body: new URLSearchParams({ username, password }),
  })
  if (!response.ok) throw new Error(`Login failed: ${response.status}`)

  response = await call('/api/auth/csrf')
  token = (await response.json()).data
  const started = performance.now()
  const timings = await Promise.all(Array.from({ length: requests }, async () => {
    const requestStarted = performance.now()
    const result = await call('/api/arrears/records?page=1&pageSize=20')
    if (result.status !== 200) throw new Error(`Concurrent request failed: ${result.status}`)
    await result.arrayBuffer()
    return performance.now() - requestStarted
  }))
  timings.sort((a, b) => a - b)
  const elapsedMs = performance.now() - started
  const p95Ms = timings[Math.max(0, Math.ceil(timings.length * 0.95) - 1)]
  process.stdout.write(`${JSON.stringify({ requests, failures: 0, elapsedMs: Math.round(elapsedMs), p95Ms: Math.round(p95Ms) })}\n`)
})().catch(error => {
  console.error(error)
  process.exitCode = 1
})
