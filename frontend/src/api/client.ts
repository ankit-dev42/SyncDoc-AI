import axios from 'axios'
import { attachWorkspaceContextHeaders } from './contextHeaders'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Interceptor to add workspace ID
apiClient.interceptors.request.use((config) => {
  return attachWorkspaceContextHeaders(config)
})

export default apiClient