import apiClient from './client'

export interface TenantInfo {
  tenantId: string
  name: string
  industry: string
  contactEmail?: string
  welcomeMessage?: string
  promptTemplate?: string
  maxConcurrentSessions?: number
  status?: string
}

export const tenantApi = {
  register: (data: TenantInfo) =>
    apiClient.post('/tenants/register', data).then((r) => r.data),

  list: () =>
    apiClient.get<TenantInfo[]>('/tenants').then((r) => r.data),

  update: (tenantId: string, data: Partial<TenantInfo>) =>
    apiClient.put(`/tenants/${tenantId}`, data).then((r) => r.data),

  deactivate: (tenantId: string) =>
    apiClient.delete(`/tenants/${tenantId}`).then((r) => r.data),
}
