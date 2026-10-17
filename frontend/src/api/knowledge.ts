import apiClient from './client'

export interface KnowledgeItem {
  knowledgeId?: string
  title: string
  content: string
  category?: string
  tags?: string
  status?: string
  sourceType?: string
}

export const knowledgeApi = {
  list: (tenantId: string, status?: string) =>
    apiClient.get('/knowledge', {
      params: { status },
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),

  add: (tenantId: string, data: KnowledgeItem) =>
    apiClient.post('/knowledge', data, {
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),

  delete: (tenantId: string, knowledgeId: string) =>
    apiClient.delete(`/knowledge/${knowledgeId}`, {
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),

  batchImport: (tenantId: string, items: KnowledgeItem[]) =>
    apiClient.post('/knowledge/batch', items, {
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),

  ensureIndex: (tenantId: string) =>
    apiClient.post('/knowledge/index/ensure', null, {
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),
}
