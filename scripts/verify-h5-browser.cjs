const { chromium } = require(process.env.PLAYWRIGHT_MODULE)

const baseUrl = process.env.H5_BASE_URL || 'http://127.0.0.1:5173'
const username = process.env.H5_E2E_USER
const password = process.env.H5_E2E_PASSWORD
if (!username || !password) throw new Error('H5_E2E_USER and H5_E2E_PASSWORD are required')

const results = []
function record(name, condition, detail = '') {
  if (!condition) throw new Error(`${name} failed${detail ? `: ${detail}` : ''}`)
  results.push({ name, detail })
}

async function login(page) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' })
  await page.getByLabel('账号').fill(username)
  await page.getByLabel('密码').fill(password)
  await Promise.all([
    page.waitForURL(url => !url.pathname.endsWith('/login')),
    page.getByRole('button', { name: '登 录' }).click(),
  ])
  record('Session login', page.url().includes('/dashboard'), page.url())
}

;(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: process.env.H5_BROWSER_PATH })
  try {
    const context = await browser.newContext({ viewport: { width: 360, height: 800 } })
    const page = await context.newPage()
    const pageErrors = []
    page.on('pageerror', error => pageErrors.push(error.message))
    await login(page)

    const dimensions = await page.evaluate(() => ({
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: document.documentElement.clientWidth,
      menuVisible: Boolean(document.querySelector('.mobile-menu-button')),
    }))
    record('360px has no page-level horizontal overflow', dimensions.documentWidth <= dimensions.viewportWidth,
      JSON.stringify(dimensions))
    record('Mobile navigation control is present', dimensions.menuVisible)

    await page.setViewportSize({ width: 800, height: 360 })
    const landscape = await page.evaluate(() => ({
      documentWidth: document.documentElement.scrollWidth,
      viewportWidth: document.documentElement.clientWidth,
    }))
    record('Mobile landscape has no page-level horizontal overflow', landscape.documentWidth <= landscape.viewportWidth,
      JSON.stringify(landscape))
    await page.setViewportSize({ width: 360, height: 800 })

    await context.setOffline(true)
    await page.waitForSelector('.network-status-banner')
    record('Offline state shows the network banner', (await page.textContent('.network-status-banner'))?.includes('院内网络已断开'))
    await context.setOffline(false)
    await page.waitForSelector('.network-status-banner', { state: 'detached' })

    const systemInfo = await page.request.get(`${baseUrl}/api/system/info`)
    const securityHeaders = systemInfo.headers()
    record('API sends CSP', securityHeaders['content-security-policy']?.includes("frame-ancestors 'none'"))
    record('API denies framing', securityHeaders['x-frame-options'] === 'DENY', securityHeaders['x-frame-options'])
    record('API sends nosniff', securityHeaders['x-content-type-options'] === 'nosniff', securityHeaders['x-content-type-options'])
    record('API restricts referrers', securityHeaders['referrer-policy'] === 'no-referrer', securityHeaders['referrer-policy'])
    record('API restricts device permissions', securityHeaders['permissions-policy']?.includes('camera=()'))

    const cookies = await context.cookies(baseUrl)
    const session = cookies.find(cookie => cookie.name === 'JSESSIONID')
    record('Session cookie exists', Boolean(session))
    record('Session cookie is HttpOnly', session?.httpOnly === true)
    record('Session cookie is SameSite Strict', session?.sameSite === 'Strict', session?.sameSite)

    let logoutRequest
    page.on('request', request => {
      if (request.url().endsWith('/api/auth/logout')) logoutRequest = request
    })
    await Promise.all([
      page.waitForURL(url => url.pathname.endsWith('/login')),
      page.getByRole('button', { name: '退出' }).click(),
    ])
    record('Logout returns to login', page.url().includes('/login'))
    record('Logout uses POST', logoutRequest?.method() === 'POST', logoutRequest?.method())
    record('Logout carries CSRF header', Boolean(logoutRequest?.headers()['x-csrf-token']))

    await login(page)
    const attack = await context.newPage()
    await attack.goto(`data:text/html,<form method="POST" action="${baseUrl}/api/auth/logout"><button>send</button></form>`, { waitUntil: 'load' })
    await Promise.all([
      attack.waitForLoadState('domcontentloaded'),
      attack.getByRole('button', { name: 'send' }).click(),
    ])
    const attackBody = await attack.textContent('body') || ''
    record('Cross-site form is rejected', /AUTH_REQUIRED|CSRF_INVALID|ORIGIN_DENIED/.test(attackBody), attackBody)
    const stillLoggedIn = await page.evaluate(async () => (await fetch('/api/system/me')).status)
    record('Cross-site form does not terminate active session', stillLoggedIn === 200, String(stillLoggedIn))
    await attack.close()

    await context.clearCookies()
    await page.goto(`${baseUrl}/dashboard`, { waitUntil: 'networkidle' })
    record('Missing/expired session redirects to login', page.url().includes('/login?reason=expired'), page.url())
    record('No unexpected browser page errors', pageErrors.length === 0, pageErrors.join(' | '))

    process.stdout.write(`${JSON.stringify({ baseUrl, passed: results.length, results }, null, 2)}\n`)
  } finally {
    await browser.close()
  }
})().catch(error => {
  console.error(error)
  process.exitCode = 1
})
