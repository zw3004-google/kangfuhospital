import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
  timestamp: string
}

export class ApiRequestError<T = unknown> extends Error {
  constructor(message: string, public readonly data?: T) { super(message); this.name = 'ApiRequestError' }
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})
http.interceptors.request.use(config=>{const token=sessionStorage.getItem('basicAuth');if(token)config.headers.Authorization=`Basic ${token}`;return config})

http.interceptors.response.use(
  response => response,
  error => { if(error.response?.status===401){sessionStorage.removeItem('basicAuth');if(location.pathname!=='/login')location.href='/login'} if(error.response?.status===403) return Promise.reject(new ApiRequestError('没有权限执行此操作',error.response?.data?.data)); if(error.response?.status===404)return Promise.reject(new ApiRequestError(error.response?.data?.message??'请求的数据不存在',error.response?.data?.data)); return Promise.reject(new ApiRequestError(error.response?.data?.message ?? (error.code==='ECONNABORTED'?'请求超时，请稍后重试':'请求失败，请稍后重试'),error.response?.data?.data)) },
)
