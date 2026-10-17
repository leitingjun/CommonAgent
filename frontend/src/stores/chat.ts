import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi, type ChatMessage } from '@/api/chat'

/**
 * 聊天状态管理
 */
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const conversationId = ref<string | null>(null)
  const loading = ref(false)
  const currentIntent = ref('')
  const currentSentiment = ref('')

  async function sendMessage(content: string, tenantId: string, userId: string) {
    loading.value = true

    // 添加用户消息到列表
    messages.value.push({
      role: 'USER',
      content,
      timestamp: new Date().toISOString(),
    })

    try {
      const response = await chatApi.send({
        tenantId,
        userId,
        conversationId: conversationId.value || undefined,
        message: content,
      })

      conversationId.value = response.conversationId
      currentIntent.value = response.intent || ''
      currentSentiment.value = response.sentiment || ''

      // 添加 Agent 回复
      messages.value.push({
        role: response.role,
        content: response.content,
        intent: response.intent,
        sentiment: response.sentiment,
        needHumanHandoff: response.needHumanHandoff,
        timestamp: response.timestamp,
      })
    } catch (error) {
      messages.value.push({
        role: 'ASSISTANT',
        content: '抱歉，服务暂时不可用，请稍后再试。',
        timestamp: new Date().toISOString(),
      })
    } finally {
      loading.value = false
    }
  }

  async function loadHistory(tenantId: string, convId: string) {
    try {
      const history = await chatApi.getHistory(tenantId, convId)
      messages.value = history.map((msg: any) => ({
        role: msg.role,
        content: msg.content,
        intent: msg.intent,
        sentiment: msg.sentiment,
        timestamp: msg.createdAt,
      }))
    } catch (error) {
      console.error('Failed to load history:', error)
    }
  }

  function clearMessages() {
    messages.value = []
    conversationId.value = null
    currentIntent.value = ''
    currentSentiment.value = ''
  }

  return {
    messages,
    conversationId,
    loading,
    currentIntent,
    currentSentiment,
    sendMessage,
    loadHistory,
    clearMessages,
  }
})
