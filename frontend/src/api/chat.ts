import apiClient from './client'

export interface ChatMessage {
  role: string
  content: string
  intent?: string
  sentiment?: string
  needHumanHandoff?: boolean
  timestamp?: string
}

export interface ChatRequest {
  tenantId: string
  userId: string
  conversationId?: string
  message: string
  messageType?: string
  userName?: string
}

export interface ChatResponse {
  conversationId: string
  messageId: string
  content: string
  role: string
  intent?: string
  sentiment?: string
  needHumanHandoff?: boolean
  handoffReason?: string
  timestamp: string
}

export const chatApi = {
  send: (data: ChatRequest) =>
    apiClient.post<ChatResponse>('/chat/send', data).then((r) => r.data),

  getHistory: (tenantId: string, conversationId: string) =>
    apiClient.get(`/chat/history/${conversationId}`, {
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),

  getConversations: (tenantId: string, userId: string) =>
    apiClient.get('/chat/conversations', {
      params: { userId },
      headers: { 'X-Tenant-Id': tenantId },
    }).then((r) => r.data),
}
