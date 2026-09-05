import axios, { type InternalAxiosRequestConfig } from 'axios'

export interface ApiResponse<T> { success: boolean; data: T; message: string | null; timestamp: string }
export class ApiRequestError<T = unknown> extends Error {
  constructor(message: string, public readonly data?: T) { super(message); this.name = 'ApiRequestError' }
}
interface Csrf { headerName: string; token: string; username: string | null }
type SecureConfig = InternalAxiosRequestConfig & { csrfRetried?: boolean; csrfUsed?: Csrf }
export const http = axios.create({ baseURL: '/api', timeout: 15_000, withCredentials: true })
let csrf: Csrf | undefined
let pending: Promise<Csrf> | undefined
let generation = 0
function clearCsrf() { csrf = undefined; pending = undefined; generation++ }
function target(config: InternalAxiosRequestConfig) {
  const url = new URL(http.getUri(config), location.origin)
  if (url.origin !== location.origin || !url.pathname.startsWith('/api/')) throw new ApiRequestError('不允许向外部地址发送系统请求')
  return url.pathname
}
const writes = (method?: string) => !['get', 'head', 'options', 'trace'].includes((method ?? 'get').toLowerCase())
async function getCsrf(): Promise<Csrf> {
  if (csrf) return csrf
  if (!pending) {
    const version = generation
    const request = http.get<ApiResponse<Csrf>>('/auth/csrf').then(response => {
      if (version !== generation) throw new ApiRequestError('登录状态已变化，请重新操作')
      csrf = response.data.data
      return csrf
    }).finally(() => { if (version === generation) pending = undefined })
    pending = request
  }
  return pending
}
http.interceptors.request.use(async (raw) => {
  const config = raw as SecureConfig
  target(config)
  if (writes(config.method)) {
    const token = await getCsrf()
    config.headers.set(token.headerName, token.token)
    config.csrfUsed = token
  }
  return config
})
http.interceptors.response.use(
  async response => {
    const path = target(response.config)
    if (path === '/api/auth/login') { clearCsrf(); await getCsrf() }
    if (path === '/api/auth/logout') clearCsrf()
    return response
  },
  async error => {
    const config = error.config as SecureConfig | undefined
    const code = error.response?.data?.data?.code
    if (error.response?.status === 403 && code === 'CSRF_INVALID' && config && !config.csrfRetried) {
      config.csrfRetried = true
      const previous = config.csrfUsed
      // Another request may already have refreshed this token; share the in-flight refresh.
      if (csrf === previous) { csrf = undefined }
      const fresh = await getCsrf()
      if (fresh.username !== previous?.username) throw new ApiRequestError('登录状态已变化，请重新操作')
      return http.request(config)
    }
    if (error.response?.status === 401) {
      clearCsrf()
      if (location.pathname !== '/login') {
        const redirect = location.pathname + location.search
        location.href = '/login?reason=expired&redirect=' + encodeURIComponent(redirect)
      }
    }
    if (error.response?.status === 403) return Promise.reject(new ApiRequestError(
      code === 'CSRF_INVALID' ? '页面安全凭证已失效，请刷新后重试' : code === 'ORIGIN_DENIED' ? '请求来源不受信任' : '没有权限执行此操作', error.response?.data?.data))
    if (error.response?.status === 409) return Promise.reject(new ApiRequestError(error.response?.data?.message ?? '记录已被其他人员更新，请刷新后重试', error.response?.data?.data))
    if (error.response?.status === 404) return Promise.reject(new ApiRequestError(error.response?.data?.message ?? '请求的数据不存在', error.response?.data?.data))
    if (error instanceof ApiRequestError) return Promise.reject(error)
    if (!error.response && typeof navigator !== 'undefined' && !navigator.onLine) return Promise.reject(new ApiRequestError('网络连接已断开，请检查院内网络'))
    return Promise.reject(new ApiRequestError(error.response?.data?.message ?? (error.code === 'ECONNABORTED' ? '请求超时，请核对操作结果后再决定是否重试' : '请求失败，请稍后重试'), error.response?.data?.data))
  },
)
