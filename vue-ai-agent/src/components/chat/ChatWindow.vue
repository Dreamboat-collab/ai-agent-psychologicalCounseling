<template>
  <div class="chat-window" :class="{ 'dark': isDark }">
    <div class="chat-container">
      <!-- 聊天消息区域 -->
      <div class="messages-container" ref="messagesRef">
        <div class="messages-list" ref="messagesListRef">
          <!-- 欢迎消息 -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-content">
              <div class="welcome-icon">
                <Robot class="w-16 h-16 text-blue-500" />
              </div>
              <h2 class="welcome-title">欢迎使用 AI Agent 智能助手</h2>
              <p class="welcome-description">
                我是您的智能对话助手，具备以下能力：
              </p>
              <ul class="feature-list">
                <li>🧠 访问专业知识库提供准确信息</li>
                <li>🛠️ 调用多种工具完成复杂任务</li>
                <li>💬 记住完整对话历史</li>
                <li>⚡ 实时流式响应体验</li>
              </ul>
              <p class="welcome-tip">
                发送消息开始对话吧！
              </p>
            </div>
          </div>

          <!-- 消息列表 -->
          <TransitionGroup name="message" tag="div">
            <MessageItem
              v-for="message in messages"
              :key="message.id"
              :message="message"
              @regenerate="handleRegenerate"
              @copy="handleCopy"
            />
          </TransitionGroup>

          <!-- 加载指示器 -->
          <div v-if="isConnecting" class="loading-indicator">
            <div class="loading-content">
              <div class="loading-spinner"></div>
              <span>AI正在思考中...</span>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="error" class="error-message">
            <el-alert
              :title="error"
              type="error"
              :closable="true"
              @close="clearError"
              show-icon
            />
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-container">
        <InputBox
          v-model="inputMessage"
          :loading="isLoading || isConnecting"
          :disabled="isConnecting"
          :placeholder="inputPlaceholder"
          @send="handleSend"
          @stop="handleStop"
        />
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button
        link
        size="small"
        @click="scrollToBottom"
        :icon="ArrowDownIcon"
      >
        滚动到底部
      </el-button>
      
      <el-button
        link
        size="small"
        @click="clearMessages"
        :icon="DeleteIcon"
      >
        清空对话
      </el-button>
      
      <el-button
        link
        size="small"
        @click="exportChat"
        :icon="ExportIcon"
      >
        导出对话
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useConfigStore } from '@/stores/config'
import MessageItem from './MessageItem.vue'
import InputBox from './InputBox.vue'
import type { ChatMessage } from '@/api/types'

// 图标
import {
  Service as Robot,
  ArrowDown as ArrowDownIcon,
  Delete as DeleteIcon,
  Download as ExportIcon
} from '@element-plus/icons-vue'

interface Props {
  conversationId?: string
  height?: string
}

interface Emits {
  (e: 'message-sent', message: string): void
  (e: 'conversation-changed', conversationId: string): void
}

const props = withDefaults(defineProps<Props>(), {
  height: '100%'
})

const emit = defineEmits<Emits>()

// Store
const chatStore = useChatStore()
const configStore = useConfigStore()

// 响应式数据
const messagesRef = ref<HTMLElement>()
const messagesListRef = ref<HTMLElement>()
const inputMessage = ref('')
const autoScroll = ref(true)

// 计算属性
const messages = computed(() => chatStore.currentMessages)
const isLoading = computed(() => chatStore.isLoading)
const isConnecting = computed(() => chatStore.isConnecting)
const error = computed(() => chatStore.error)
const isDark = computed(() => configStore.theme.mode === 'dark')

const inputPlaceholder = computed(() => {
  if (isConnecting.value) return 'AI正在回复中...'
  if (isLoading.value) return '发送中...'
  return '输入消息...'
})

// 方法
const handleSend = async (message: string) => {
  if (!message.trim() || isConnecting.value) return

  inputMessage.value = ''
  emit('message-sent', message)

  try {
    // 使用流式发送
    chatStore.sendStreamMessage(message, props.conversationId)
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('Send message failed:', error)
    ElMessage.error('发送消息失败')
  }
}

const handleStop = () => {
  chatStore.stopStreaming()
  ElMessage.info('已停止生成')
}

const handleRegenerate = async (messageId: string) => {
  try {
    await ElMessageBox.confirm(
      '确定要重新生成这条消息吗？',
      '确认重新生成',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 找到要重新生成的消息
    const message = messages.value.find(m => m.id === messageId)
    if (!message) return

    // 找到对应的用户消息
    const messageIndex = messages.value.findIndex(m => m.id === messageId)
    if (messageIndex > 0) {
      const userMessage = messages.value[messageIndex - 1]
      if (userMessage.role === 'user') {
        // 删除助手消息，重新发送用户消息
        const conversation = chatStore.currentConversation
        if (conversation) {
          conversation.messages = conversation.messages.slice(0, messageIndex)
          handleSend(userMessage.content)
        }
      }
    }
  } catch {
    // 用户取消
  }
}

const handleCopy = (content: string) => {
  console.log('Copied:', content)
}

const clearError = () => {
  chatStore.error = null
}

const clearMessages = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空当前对话吗？此操作不可撤销。',
      '确认清空',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const conversation = chatStore.currentConversation
    if (conversation) {
      conversation.messages = []
      conversation.updatedAt = Date.now()
    }
    
    ElMessage.success('对话已清空')
  } catch {
    // 用户取消
  }
}

const exportChat = () => {
  chatStore.exportConversations()
  ElMessage.success('对话已导出')
}

const scrollToBottom = (smooth = true) => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTo({
        top: messagesRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

// 监听器
watch(messages, () => {
  if (autoScroll.value) {
    scrollToBottom()
  }
}, { deep: true })

watch(() => props.conversationId, (newId) => {
  if (newId) {
    chatStore.switchConversation(newId)
    emit('conversation-changed', newId)
    scrollToBottom(false)
  }
})

// 滚动事件处理
const handleScroll = () => {
  if (!messagesRef.value) return
  
  const { scrollTop, scrollHeight, clientHeight } = messagesRef.value
  const isAtBottom = scrollHeight - scrollTop - clientHeight < 50
  autoScroll.value = isAtBottom
}

// 生命周期
onMounted(() => {
  if (messagesRef.value) {
    messagesRef.value.addEventListener('scroll', handleScroll)
  }
  
  // 如果有conversationId，切换到该对话
  if (props.conversationId) {
    chatStore.switchConversation(props.conversationId)
  }
  
  scrollToBottom(false)
})

onUnmounted(() => {
  if (messagesRef.value) {
    messagesRef.value.removeEventListener('scroll', handleScroll)
  }
  
  // 清理连接
  chatStore.stopStreaming()
})
</script>

<style scoped>
.chat-window {
  @apply h-full flex flex-col bg-gray-50 dark:bg-gray-900;
}

.chat-container {
  @apply flex-1 flex flex-col min-h-0;
}

/* 消息容器 */
.messages-container {
  @apply flex-1 overflow-y-auto scroll-smooth;
  scrollbar-width: thin;
  scrollbar-color: theme('colors.gray.300') transparent;
}

.messages-container::-webkit-scrollbar {
  @apply w-2;
}

.messages-container::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400 dark:bg-gray-500;
}

.messages-list {
  @apply p-4 space-y-4 max-w-4xl mx-auto w-full;
}

/* 欢迎消息 */
.welcome-message {
  @apply flex items-center justify-center min-h-96;
}

.welcome-content {
  @apply text-center max-w-md;
}

.welcome-icon {
  @apply mb-6;
}

.welcome-title {
  @apply text-2xl font-bold text-gray-900 dark:text-gray-100 mb-4;
}

.welcome-description {
  @apply text-gray-600 dark:text-gray-400 mb-4;
}

.feature-list {
  @apply text-left space-y-2 mb-6;
}

.feature-list li {
  @apply text-gray-700 dark:text-gray-300;
}

.welcome-tip {
  @apply text-sm text-gray-500 dark:text-gray-400 italic;
}

/* 加载指示器 */
.loading-indicator {
  @apply flex justify-center py-4;
}

.loading-content {
  @apply flex items-center gap-3 text-gray-600 dark:text-gray-400;
}

.loading-spinner {
  @apply w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 错误消息 */
.error-message {
  @apply px-4 py-2;
}

/* 输入容器 */
.input-container {
  @apply border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4;
}

/* 工具栏 */
.toolbar {
  @apply flex items-center justify-center gap-2 p-2 bg-white dark:bg-gray-800;
  @apply border-t border-gray-200 dark:border-gray-700;
}

/* 消息过渡动画 */
.message-enter-active,
.message-leave-active {
  transition: all 0.3s ease;
}

.message-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.message-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.message-move {
  transition: transform 0.3s ease;
}
</style> 