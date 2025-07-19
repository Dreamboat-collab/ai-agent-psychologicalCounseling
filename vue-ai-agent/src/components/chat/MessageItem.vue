<template>
  <div 
    class="message-item animate-slideUp"
    :class="[
      `message-${message.role}`,
      { 'message-streaming': message.isStreaming }
    ]"
  >
    <!-- 工具思考消息 -->
    <ToolThinkingBox
      v-if="message.role === 'tool_thinking'"
      :message="message"
      @copy="handleCopy"
    />
    
    <!-- 普通消息 -->
    <div v-else class="message-wrapper">
      <!-- 头像 -->
      <div class="message-avatar">
        <div class="avatar-container">
          <component 
            :is="avatarIcon" 
            class="w-6 h-6"
            :class="avatarColorClass"
          />
        </div>
      </div>

      <!-- 消息内容 -->
      <div class="message-content">
        <!-- 消息头部 -->
        <div class="message-header">
          <span class="role-name">{{ roleName }}</span>
          <span class="timestamp">{{ formattedTime }}</span>
          
          <!-- 状态指示器 -->
          <div v-if="message.isStreaming" class="status-indicator">
            <div class="typing-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>

        <!-- 消息体 -->
        <div class="message-body">
          <!-- 直接显示内容，不使用前端打字机效果（后端已经实现了流式传输） -->
          <div
            class="message-text"
            v-html="formattedContent"
          />
        </div>

        <!-- 消息操作 -->
        <div v-if="showActions" class="message-actions">
          <el-button
            link
            size="small"
            @click="copyMessage"
            :icon="CopyIcon"
          >
            复制
          </el-button>
          
          <el-button
            v-if="message.role === 'assistant'"
            link
            size="small"
            @click="regenerateMessage"
            :icon="RefreshIcon"
          >
            重新生成
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import dayjs from 'dayjs'
import type { ChatMessage } from '@/api/types'
import ToolThinkingBox from './ToolThinkingBox.vue'

// 图标（这里使用Element Plus的图标）
import { 
  User as UserIcon,
  Service as RobotIcon,
  Setting as SystemIcon,
  CopyDocument as CopyIcon,
  Refresh as RefreshIcon
} from '@element-plus/icons-vue'

interface Props {
  message: ChatMessage
  showActions?: boolean
  showAvatar?: boolean
}

interface Emits {
  (e: 'regenerate', messageId: string): void
  (e: 'copy', content: string): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true,
  showAvatar: true
})

const emit = defineEmits<Emits>()

// 角色相关计算属性
const roleName = computed(() => {
  const names = {
    user: '用户',
    assistant: 'AI助手',
    system: '系统',
    tool_thinking: '工具思考'
  }
  return names[props.message.role] || props.message.role
})

const avatarIcon = computed(() => {
  const icons = {
    user: UserIcon,
    assistant: RobotIcon,
    system: SystemIcon,
    tool_thinking: RobotIcon
  }
  return icons[props.message.role] || UserIcon
})

const avatarColorClass = computed(() => {
  const colors = {
    user: 'text-blue-600 dark:text-blue-400',
    assistant: 'text-green-600 dark:text-green-400',
    system: 'text-gray-600 dark:text-gray-400',
    tool_thinking: 'text-orange-600 dark:text-orange-400'
  }
  return colors[props.message.role] || 'text-gray-600'
})

// 格式化时间
const formattedTime = computed(() => {
  return dayjs(props.message.timestamp).format('HH:mm:ss')
})

// 格式化消息内容
const formattedContent = computed(() => {
  if (!props.message.content) return ''
  
  try {
    return marked(props.message.content, {
      breaks: true,
      gfm: true
    })
  } catch (error) {
    console.warn('Markdown parse error:', error)
    return props.message.content.replace(/\n/g, '<br>')
  }
})

// 事件处理
const copyMessage = async () => {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('已复制到剪贴板')
    emit('copy', props.message.content)
  } catch (error) {
    console.error('Copy failed:', error)
    ElMessage.error('复制失败')
  }
}

const handleCopy = (content: string) => {
  emit('copy', content)
}

const regenerateMessage = () => {
  emit('regenerate', props.message.id)
}
</script>

<style scoped>
.message-item {
  @apply mb-4 last:mb-0;
}

/* 工具思考消息样式 */
.message-tool_thinking {
  @apply w-full max-w-4xl mx-auto;
}

.message-wrapper {
  @apply flex gap-3 max-w-full;
}

.message-user .message-wrapper {
  @apply flex-row-reverse;
}

/* 头像样式 */
.message-avatar {
  @apply flex-shrink-0;
}

.avatar-container {
  @apply w-10 h-10 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center;
  @apply border-2 border-gray-200 dark:border-gray-700;
}

.message-assistant .avatar-container {
  @apply bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800;
}

.message-user .avatar-container {
  @apply bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800;
}

/* 消息内容样式 */
.message-content {
  @apply flex-1 min-w-0;
}

.message-user .message-content {
  @apply text-right;
}

.message-header {
  @apply flex items-center gap-2 mb-1;
}

.message-user .message-header {
  @apply justify-end;
}

.role-name {
  @apply text-sm font-medium text-gray-900 dark:text-gray-100;
}

.timestamp {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

.status-indicator {
  @apply flex items-center;
}

/* 输入指示器 */
.typing-dots {
  @apply flex gap-1;
}

.typing-dots span {
  @apply w-1.5 h-1.5 bg-green-500 rounded-full;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 消息体样式 */
.message-body {
  @apply rounded-lg px-4 py-3 max-w-prose;
  @apply bg-white dark:bg-gray-800;
  @apply border border-gray-200 dark:border-gray-700;
  @apply shadow-sm;
}

.message-user .message-body {
  @apply bg-blue-500 text-white border-blue-500;
  @apply ml-auto;
}

.message-assistant .message-body {
  @apply bg-white dark:bg-gray-800;
}

.message-streaming .message-body {
  @apply relative;
}

.message-streaming .message-body::after {
  content: '';
  @apply absolute -bottom-1 -right-1 w-2 h-2;
  @apply bg-green-400 rounded-full;
  animation: pulse 2s infinite;
}

/* 消息文本样式 */
.message-text {
  @apply leading-relaxed;
}

.message-user .message-text {
  @apply text-white;
}

/* 消息操作 */
.message-actions {
  @apply flex gap-1 mt-2 opacity-0 transition-opacity;
}

.message-item:hover .message-actions {
  @apply opacity-100;
}

.message-user .message-actions {
  @apply justify-end;
}

/* 动画 */
@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.animate-slideUp {
  animation: slideUp 0.3s ease-out;
}

/* Markdown样式覆盖 */
.message-body :deep(p) {
  @apply mb-2 last:mb-0;
}

.message-body :deep(code) {
  @apply bg-gray-100 dark:bg-gray-700 px-1 py-0.5 rounded text-sm;
}

.message-user .message-body :deep(code) {
  @apply bg-blue-600 text-blue-100;
}

.message-body :deep(pre) {
  @apply bg-gray-100 dark:bg-gray-700 p-3 rounded overflow-x-auto;
}

.message-user .message-body :deep(pre) {
  @apply bg-blue-600;
}

.message-body :deep(a) {
  @apply underline;
}

.message-user .message-body :deep(a) {
  @apply text-blue-100;
}
</style> 