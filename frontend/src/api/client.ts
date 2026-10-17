import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 - 添加租户 ID
apiClient.interceptors.request.use((config) => {
  const tenantId = localStorage.getItem('tenantId') || 'demo'
  config.headers['X-Tenant-Id'] = tenantId
  return config
})

// 响应拦截器 - 错误处理
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default apiClient
