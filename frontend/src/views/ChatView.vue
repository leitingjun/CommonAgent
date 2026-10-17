<template>
  <div class="chat-container">
    <!-- 侧边栏 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h2>🤖 CommonAgent</h2>
        <p class="subtitle">通用智能客服平台</p>
      </div>
      <div class="tenant-selector">
        <el-select v-model="appStore.tenantId" placeholder="选择租户" size="small">
          <el-option label="演示公司" value="demo" />
          <el-option label="示例电商" value="shop123" />
          <el-option label="示例金融" value="bank456" />
        </el-select>
      </div>
      <div class="sidebar-actions">
        <el-button type="primary" size="small" @click="chatStore.clearMessages()" :icon="'Plus'">
          新建对话
        </el-button>
      </div>
      <div class="sidebar-footer">
        <el-button text size="small" @click="$router.push('/admin')">
          管理后台
        </el-button>
      </div>
    </aside>

    <!-- 聊天主区域 -->
    <main class="chat-main">
      <!-- 顶部状态栏 -->
      <header class="chat-header">
        <div class="header-info">
          <span class="tenant-badge">{{ appStore.tenantId }}</span>
          <span v-if="chatStore.currentIntent" class="intent-badge">
            {{ intentLabel }}
          </span>
          <span v-if="chatStore.currentSentiment" class="sentiment-badge" :class="sentimentClass">
            {{ sentimentLabel }}
          </span>
        </div>
      </header>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="chatStore.messages.length === 0" class="empty-chat">
          <div class="empty-icon">💬</div>
          <h3>开始对话</h3>
          <p>我是您的智能客服助手，有什么可以帮助您的？</p>
          <div class="quick-actions">
            <el-button
              v-for="action in quickActions"
              :key="action"
              size="small"
              round
              @click="sendQuickAction(action)"
            >
              {{ action }}
            </el-button>
          </div>
        </div>

        <div
          v-for="(msg, index) in chatStore.messages"
          :key="index"
          class="message-wrapper"
          :class="msg.role === 'USER' ? 'user-message' : 'agent-message'"
        >
          <div class="message-avatar">
            <el-avatar :size="36" :icon="msg.role === 'USER' ? 'UserFilled' : 'Service'" />
          </div>
          <div class="message-body">
            <div class="message-header">
              <span class="message-role">{{ msg.role === 'USER' ? '我' : '智能助手' }}</span>
              <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
            </div>
            <div class="message-content" v-html="renderContent(msg.content)"></div>
            <div v-if="msg.needHumanHandoff" class="transfer-notice">
              🔄 已转接人工客服
            </div>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="chatStore.loading" class="message-wrapper agent-message">
          <div class="message-avatar">
            <el-avatar :size="36" icon="Service" />
          </div>
          <div class="message-body">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <footer class="chat-input">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入您的问题..."
          :disabled="chatStore.loading"
          @keydown.enter.exact="handleSend"
          resize="none"
        />
        <el-button
          type="primary"
          :icon="'Promotion'"
          :loading="chatStore.loading"
          @click="handleSend"
          :disabled="!inputText.trim()"
        >
          发送
        </el-button>
      </footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { useChatStore } from '@/stores/chat'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ breaks: true })

const appStore = useAppStore()
const chatStore = useChatStore()
const inputText = ref('')
const messagesContainer = ref<HTMLElement>()

const quickActions = [
  '查询我的订单',
  '如何退货？',
  '联系人工客服',
  '物流到哪里了？',
  '有什么优惠活动？',
]

const intentLabels: Record<string, string> = {
  INQUIRY: '📋 咨询',
  COMPLAINT: '😤 投诉',
  ORDER_QUERY: '📦 订单查询',
  REFUND: '💰 退款',
  TECHNICAL_SUPPORT: '🔧 技术支持',
  GREETING: '👋 问候',
  OTHER: '💬 其他',
}

const sentimentLabels: Record<string, string> = {
  POSITIVE: '😊 积极',
  NEUTRAL: '😐 中性',
  NEGATIVE: '😞 消极',
}

const intentLabel = computed(() => intentLabels[chatStore.currentIntent] || chatStore.currentIntent)
const sentimentLabel = computed(() => sentimentLabels[chatStore.currentSentiment] || chatStore.currentSentiment)
const sentimentClass = computed(() => chatStore.currentSentiment?.toLowerCase() || '')

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || chatStore.loading) return

  inputText.value = ''
  await chatStore.sendMessage(text, appStore.tenantId, appStore.userId)
  await scrollToBottom()
}

function sendQuickAction(action: string) {
  inputText.value = action
  handleSend()
}

function renderContent(content: string): string {
  if (!content) return ''
  try {
    return md.render(content)
  } catch {
    return content.replace(/\n/g, '<br>')
  }
}

function formatTime(timestamp?: string): string {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.chat-sidebar {
  width: 240px;
  background: #1a1a2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  padding: 20px;
  flex-shrink: 0;

  .sidebar-header {
    margin-bottom: 24px;
    h2 { font-size: 20px; margin: 0; }
    .subtitle { font-size: 12px; color: #8890a0; margin: 4px 0 0; }
  }

  .tenant-selector {
    margin-bottom: 16px;
    :deep(.el-select) { width: 100%; }
  }

  .sidebar-actions { margin-bottom: 16px; }

  .sidebar-footer {
    margin-top: auto;
    :deep(.el-button) { color: #8890a0; }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  .header-info {
    display: flex;
    gap: 8px;
    align-items: center;
  }
  .tenant-badge {
    font-size: 12px;
    background: #ecf5ff;
    color: #409eff;
    padding: 2px 10px;
    border-radius: 12px;
  }
  .intent-badge {
    font-size: 12px;
    background: #f0f9eb;
    color: #67c23a;
    padding: 2px 10px;
    border-radius: 12px;
  }
  .sentiment-badge {
    font-size: 12px;
    padding: 2px 10px;
    border-radius: 12px;
    &.positive { background: #f0f9eb; color: #67c23a; }
    &.neutral { background: #fdf6ec; color: #e6a23c; }
    &.negative { background: #fef0f0; color: #f56c6c; }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;

  .empty-chat {
    text-align: center;
    padding: 60px 20px;
    .empty-icon { font-size: 64px; margin-bottom: 16px; }
    h3 { font-size: 18px; color: #303133; margin: 0 0 8px; }
    p { color: #909399; margin: 0 0 24px; }
    .quick-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
    }
  }
}

.message-wrapper {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 80%;

  &.user-message {
    margin-left: auto;
    flex-direction: row-reverse;
    .message-body {
      background: #409eff;
      color: #fff;
      border-radius: 12px 4px 12px 12px;
    }
    .message-header { color: rgba(255,255,255,0.8); }
  }

  &.agent-message {
    .message-body {
      background: #fff;
      border-radius: 4px 12px 12px 12px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    }
  }
}

.message-avatar { flex-shrink: 0; }

.message-body {
  padding: 12px 16px;
  min-width: 120px;

  .message-header {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    margin-bottom: 6px;
    .message-role { font-weight: 600; }
    .message-time { opacity: 0.7; }
  }

  .message-content {
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
    :deep(p) { margin: 4px 0; }
    :deep(ul), :deep(ol) { padding-left: 20px; }
  }

  .transfer-notice {
    margin-top: 8px;
    padding: 6px 12px;
    background: #fdf6ec;
    border-radius: 6px;
    font-size: 12px;
    color: #e6a23c;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #909399;
    animation: typing 1.4s infinite ease-in-out;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}

.chat-input {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    font-size: 14px;
  }
  .el-button {
    border-radius: 12px;
    height: 44px;
  }
}
</style>
