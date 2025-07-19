<template>
  <div class="input-box">
    <div class="input-container">
      <!-- 文件上传区域（预留） -->
      <div v-if="showAttachment" class="attachment-area">
        <el-button
          link
          :icon="PaperclipIcon"
          @click="handleAttachment"
          title="附件"
        />
      </div>

      <!-- 文本输入区域 -->
      <div class="text-input-area">
        <el-input
          ref="inputRef"
          v-model="localMessage"
          type="textarea"
          :placeholder="placeholder"
          :disabled="disabled"
          :autosize="{ minRows: 1, maxRows: 6 }"
          :maxlength="maxLength"
          show-word-limit
          resize="none"
          @keydown="handleKeydown"
          @input="handleInput"
          @focus="handleFocus"
          @blur="handleBlur"
          class="message-input"
        />
        
        <!-- 快捷操作按钮 -->
        <div class="quick-actions" v-show="isFocused || localMessage.trim()">
          <el-button
            v-if="showClear"
            link
            size="small"
            @click="clearInput"
            :icon="ClearIcon"
            title="清空"
          />
          
          <el-button
            v-if="showEmojiPicker"
            link
            size="small"
            @click="toggleEmojiPicker"
            :icon="EmojiIcon"
            title="表情"
          />
        </div>
      </div>

      <!-- 发送/停止按钮 -->
      <div class="send-button-area">
        <el-button
          v-if="!loading"
          type="primary"
          :disabled="!canSend"
          @click="handleSend"
          :icon="SendIcon"
          class="send-button"
        >
          发送
        </el-button>
        
        <el-button
          v-else
          type="danger"
          @click="handleStop"
          :icon="StopIcon"
          class="stop-button"
        >
          停止
        </el-button>
      </div>
    </div>

    <!-- 表情选择器（预留） -->
    <div v-if="showEmojiPanel" class="emoji-panel">
      <div class="emoji-grid">
        <span
          v-for="emoji in commonEmojis"
          :key="emoji"
          class="emoji-item"
          @click="insertEmoji(emoji)"
        >
          {{ emoji }}
        </span>
      </div>
    </div>

    <!-- 输入提示 -->
    <div v-if="showHints" class="input-hints">
      <div class="hints-content">
        <el-tag size="small" type="info">Ctrl + Enter 发送</el-tag>
        <el-tag size="small" type="info">Shift + Enter 换行</el-tag>
        <el-tag size="small" type="info">/ 开头触发命令</el-tag>
      </div>
    </div>

    <!-- 字符计数 -->
    <div v-if="showWordCount" class="word-count">
      {{ localMessage.length }} / {{ maxLength }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// 图标
import {
  ChatDotRound as SendIcon,
  VideoPause as StopIcon,
  Paperclip as PaperclipIcon,
  Delete as ClearIcon,
  Star as EmojiIcon
} from '@element-plus/icons-vue'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  loading?: boolean
  maxLength?: number
  showAttachment?: boolean
  showEmojiPicker?: boolean
  showClear?: boolean
  showHints?: boolean
  showWordCount?: boolean
  autoFocus?: boolean
  allowEmpty?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'send', message: string): void
  (e: 'stop'): void
  (e: 'focus'): void
  (e: 'blur'): void
  (e: 'input', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '输入消息...',
  disabled: false,
  loading: false,
  maxLength: 2000,
  showAttachment: false,
  showEmojiPicker: true,
  showClear: true,
  showHints: false,
  showWordCount: false,
  autoFocus: true,
  allowEmpty: false
})

const emit = defineEmits<Emits>()

// 响应式数据
const inputRef = ref()
const localMessage = ref(props.modelValue)
const isFocused = ref(false)
const showEmojiPanel = ref(false)

// 常用表情
const commonEmojis = [
  '😊', '😂', '😍', '🤔', '😢', '😡', '👍', '👎',
  '❤️', '💯', '🔥', '⭐', '✅', '❌', '🎉', '💡'
]

// 计算属性
const canSend = computed(() => {
  return !props.disabled && 
         !props.loading && 
         (props.allowEmpty || localMessage.value.trim().length > 0)
})

// 监听器
watch(() => props.modelValue, (newValue) => {
  localMessage.value = newValue
})

watch(localMessage, (newValue) => {
  emit('update:modelValue', newValue)
})

// 方法
const handleSend = () => {
  const message = localMessage.value.trim()
  if (!message && !props.allowEmpty) {
    ElMessage.warning('请输入消息内容')
    return
  }

  emit('send', message)
  localMessage.value = ''
  focusInput()
}

const handleStop = () => {
  emit('stop')
}

const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl + Enter 发送
  if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
    event.preventDefault()
    handleSend()
    return
  }

  // Shift + Enter 换行（默认行为，不需要处理）
  
  // Enter 发送（当不按 Shift 时）
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
    return
  }

  // Escape 清空输入
  if (event.key === 'Escape') {
    event.preventDefault()
    clearInput()
    return
  }
}

const handleInput = (value: string) => {
  emit('input', value)
}

const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

const handleBlur = () => {
  isFocused.value = false
  emit('blur')
}

const clearInput = () => {
  localMessage.value = ''
  focusInput()
}

const focusInput = () => {
  nextTick(() => {
    inputRef.value?.focus()
  })
}

const handleAttachment = () => {
  ElMessage.info('附件功能开发中...')
}

const toggleEmojiPicker = () => {
  showEmojiPanel.value = !showEmojiPanel.value
}

const insertEmoji = (emoji: string) => {
  const input = inputRef.value?.textarea || inputRef.value?.input
  if (input) {
    const start = input.selectionStart
    const end = input.selectionEnd
    const text = localMessage.value
    
    localMessage.value = text.slice(0, start) + emoji + text.slice(end)
    
    nextTick(() => {
      const newPos = start + emoji.length
      input.setSelectionRange(newPos, newPos)
      input.focus()
    })
  } else {
    localMessage.value += emoji
  }
  
  showEmojiPanel.value = false
}

// 生命周期
onMounted(() => {
  if (props.autoFocus) {
    focusInput()
  }
})

// 暴露方法
defineExpose({
  focus: focusInput,
  clear: clearInput,
  insertText: (text: string) => {
    localMessage.value += text
  }
})
</script>

<style scoped>
.input-box {
  @apply relative;
}

.input-container {
  @apply flex items-end gap-3 p-4 bg-white dark:bg-gray-800 rounded-lg;
  @apply border border-gray-200 dark:border-gray-700;
  @apply shadow-sm;
}

.attachment-area {
  @apply flex items-center;
}

.text-input-area {
  @apply flex-1 relative;
}

.message-input {
  @apply w-full;
}

.message-input :deep(.el-textarea__inner) {
  @apply border-0 p-0 resize-none shadow-none;
  @apply bg-transparent text-gray-900 dark:text-gray-100;
  @apply placeholder-gray-400 dark:placeholder-gray-500;
  line-height: 1.5;
}

.message-input :deep(.el-textarea__inner):focus {
  @apply border-0 shadow-none;
}

.message-input :deep(.el-input__count) {
  @apply text-xs text-gray-400 bg-transparent;
}

.quick-actions {
  @apply absolute right-2 top-1/2 transform -translate-y-1/2;
  @apply flex items-center gap-1;
}

.send-button-area {
  @apply flex items-center;
}

.send-button,
.stop-button {
  @apply px-4 py-2 font-medium;
  @apply transition-all duration-200;
}

.send-button {
  @apply bg-blue-500 hover:bg-blue-600;
  @apply disabled:bg-gray-300 disabled:cursor-not-allowed;
}

.stop-button {
  @apply bg-red-500 hover:bg-red-600;
}

/* 表情面板 */
.emoji-panel {
  @apply absolute bottom-full left-0 right-0 mb-2;
  @apply bg-white dark:bg-gray-800 rounded-lg shadow-lg;
  @apply border border-gray-200 dark:border-gray-700;
  @apply p-3 z-10;
}

.emoji-grid {
  @apply grid grid-cols-8 gap-2;
}

.emoji-item {
  @apply w-8 h-8 flex items-center justify-center;
  @apply cursor-pointer rounded hover:bg-gray-100 dark:hover:bg-gray-700;
  @apply text-lg transition-colors;
}

/* 输入提示 */
.input-hints {
  @apply mt-2;
}

.hints-content {
  @apply flex flex-wrap gap-2;
}

/* 字符计数 */
.word-count {
  @apply absolute -top-6 right-0;
  @apply text-xs text-gray-400;
}

/* 焦点状态 */
.input-container:focus-within {
  @apply border-blue-500 dark:border-blue-400;
  @apply shadow-md;
}

/* 禁用状态 */
.input-container.disabled {
  @apply bg-gray-50 dark:bg-gray-900;
  @apply border-gray-300 dark:border-gray-600;
  @apply cursor-not-allowed;
}

/* 加载状态 */
.input-container.loading {
  @apply relative;
}

.input-container.loading::after {
  content: '';
  @apply absolute inset-0 bg-white/50 dark:bg-gray-800/50;
  @apply pointer-events-none;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .input-container {
    @apply p-3 gap-2;
  }
  
  .send-button,
  .stop-button {
    @apply px-3 py-2 text-sm;
  }
  
  .quick-actions {
    @apply right-1;
  }
}
</style> 