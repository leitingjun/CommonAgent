import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局应用状态 - 租户和用户信息
 */
export const useAppStore = defineStore('app', () => {
  const tenantId = ref(localStorage.getItem('tenantId') || 'demo')
  const userId = ref(localStorage.getItem('userId') || generateUserId())
  const userName = ref(localStorage.getItem('userName') || '用户')

  function generateUserId(): string {
    const id = 'user_' + Math.random().toString(36).substring(2, 10)
    localStorage.setItem('userId', id)
    return id
  }

  function setTenant(id: string) {
    tenantId.value = id
    localStorage.setItem('tenantId', id)
  }

  function setUser(id: string, name: string) {
    userId.value = id
    userName.value = name
    localStorage.setItem('userId', id)
    localStorage.setItem('userName', name)
  }

  return { tenantId, userId, userName, setTenant, setUser }
})
