import axios from 'axios'
import { attachWorkspaceContextHeaders } from './contextHeaders'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Attach JWT Bearer token + workspace context headers
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return attachWorkspaceContextHeaders(config)
})

// On 401 for auth endpoints specifically, clear the token
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error?.config?.url ?? ''
    // Only force-logout when the token itself is rejected (not workspace/data 401s)
    if (error?.response?.status === 401 && !url.includes('/auth/')) {
      const token = localStorage.getItem('accessToken')
      // Only clear if the request carried a token (meaning the token was rejected)
      if (token && error?.config?.headers?.Authorization) {
        localStorage.removeItem('accessToken')
        window.dispatchEvent(new Event('storage'))
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient