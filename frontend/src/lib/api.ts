import axios, { type AxiosError } from 'axios'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8091/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshPromise: Promise<void> | null = null

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const request = error.config
    const url = request?.url || ''
    const authEndpoint = /^\/auth\/(login|register|refresh|logout|password-reset\/(?:request|complete))(?:\/|$)/.test(url)

    if (error.response?.status !== 401 || !request || authEndpoint) {
      return Promise.reject(error)
    }

    if (request.headers.get('X-Auth-Retry') === 'true') {
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
      return Promise.reject(error)
    }

    request.headers.set('X-Auth-Retry', 'true')
    refreshPromise ??= api.post('/auth/refresh')
      .then(() => undefined)
      .finally(() => { refreshPromise = null })

    try {
      await refreshPromise
      return await api.request(request)
    } catch (refreshError) {
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
      return Promise.reject(refreshError)
    }
  },
)
