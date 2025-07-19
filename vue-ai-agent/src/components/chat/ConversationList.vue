<template>
  <div class="conversation-list">
    <!-- 头部操作区 -->
    <div class="list-header">
      <h2 class="list-title">对话列表</h2>
      <el-button
        type="primary"
        size="small"
        :icon="PlusIcon"
        @click="handleCreate"
        class="create-button"
      >
        新建对话
      </el-button>
    </div>

    <!-- 搜索框 -->
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索对话..."
        :prefix-icon="SearchIcon"
        clearable
        size="small"
      />
    </div>

    <!-- 对话列表 -->
    <div class="conversations-container">
      <div v-if="filteredConversations.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <p class="empty-text">
          {{ searchQuery ? '没有找到匹配的对话' : '还没有对话，点击上方按钮开始' }}
        </p>
      </div>

      <div v-else class="conversations-list">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{ 'active': conversation.id === currentId }"
          @click="handleSelect(conversation.id)"
        >
          <!-- 对话信息 -->
          <div class="conversation-content">
            <div class="conversation-title">
              <span class="title-text">{{ conversation.title }}</span>
              <div class="conversation-meta">
                <span class="message-count">{{ conversation.messages.length }} 条消息</span>
                <span class="last-update">{{ formatTime(conversation.updatedAt) }}</span>
              </div>
            </div>
            
            <!-- 最后一条消息预览 -->
            <div v-if="getLastMessage(conversation)" class="last-message">
              <span class="message-role">
                {{ getLastMessage(conversation)?.role === 'user' ? '你' : 'AI' }}:
              </span>
              <span class="message-preview">
                {{ truncateText(getLastMessage(conversation)?.content || '', 50) }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="conversation-actions" @click.stop>
            <el-dropdown
              trigger="click"
              placement="bottom-end"
              @command="handleCommand"
            >
              <el-button
                link
                size="small"
                :icon="MoreIcon"
                class="action-button"
              />
              
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item 
                    :command="{ action: 'rename', id: conversation.id }"
                    :icon="EditIcon"
                  >
                    重命名
                  </el-dropdown-item>
                  <el-dropdown-item 
                    :command="{ action: 'export', id: conversation.id }"
                    :icon="ExportIcon"
                  >
                    导出
                  </el-dropdown-item>
                  <el-dropdown-item 
                    :command="{ action: 'delete', id: conversation.id }"
                    :icon="DeleteIcon"
                    divided
                    class="delete-item"
                  >
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog
      v-model="showRenameDialog"
      title="重命名对话"
      width="400px"
      :before-close="handleCloseRename"
    >
      <el-input
        v-model="newTitle"
        placeholder="请输入新的对话标题"
        maxlength="50"
        show-word-limit
        @keyup.enter="handleConfirmRename"
        ref="renameInputRef"
      />
      
      <template #footer>
        <el-button @click="handleCloseRename">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirmRename"
          :disabled="!newTitle.trim()"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import type { Conversation } from '@/api/types'

// 图标
import {
  Plus as PlusIcon,
  Search as SearchIcon,
  MoreFilled as MoreIcon,
  Edit as EditIcon,
  Download as ExportIcon,
  Delete as DeleteIcon
} from '@element-plus/icons-vue'

// 启用相对时间插件
dayjs.extend(relativeTime)

interface Props {
  conversations: Conversation[]
  currentId: string
}

interface Emits {
  (e: 'select', id: string): void
  (e: 'create'): void
  (e: 'delete', id: string): void
  (e: 'rename', id: string, newTitle: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const searchQuery = ref('')
const showRenameDialog = ref(false)
const newTitle = ref('')
const renamingId = ref('')
const renameInputRef = ref()

// 计算属性
const filteredConversations = computed(() => {
  if (!searchQuery.value.trim()) {
    return props.conversations
  }
  
  const query = searchQuery.value.toLowerCase()
  return props.conversations.filter(conversation => 
    conversation.title.toLowerCase().includes(query) ||
    conversation.messages.some(message => 
      message.content.toLowerCase().includes(query)
    )
  )
})

// 工具函数
const formatTime = (timestamp: number): string => {
  const now = dayjs()
  const time = dayjs(timestamp)
  const diffHours = now.diff(time, 'hour')
  
  if (diffHours < 24) {
    return time.format('HH:mm')
  } else if (diffHours < 24 * 7) {
    return time.format('MM-DD')
  } else {
    return time.format('YYYY-MM-DD')
  }
}

const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength) + '...'
}

const getLastMessage = (conversation: Conversation) => {
  return conversation.messages[conversation.messages.length - 1]
}

// 事件处理
const handleSelect = (id: string) => {
  emit('select', id)
}

const handleCreate = () => {
  emit('create')
}

const handleCommand = async ({ action, id }: { action: string; id: string }) => {
  switch (action) {
    case 'rename':
      await handleRename(id)
      break
    case 'export':
      handleExport(id)
      break
    case 'delete':
      await handleDelete(id)
      break
  }
}

const handleRename = async (id: string) => {
  const conversation = props.conversations.find(c => c.id === id)
  if (!conversation) return
  
  renamingId.value = id
  newTitle.value = conversation.title
  showRenameDialog.value = true
  
  // 等待对话框打开后聚焦输入框
  await nextTick()
  renameInputRef.value?.focus()
  renameInputRef.value?.select()
}

const handleConfirmRename = () => {
  if (!newTitle.value.trim()) {
    ElMessage.warning('请输入对话标题')
    return
  }
  
  emit('rename', renamingId.value, newTitle.value.trim())
  handleCloseRename()
}

const handleCloseRename = () => {
  showRenameDialog.value = false
  newTitle.value = ''
  renamingId.value = ''
}

const handleExport = (id: string) => {
  const conversation = props.conversations.find(c => c.id === id)
  if (!conversation) return
  
  const data = {
    conversation,
    exportTime: Date.now(),
    version: '1.0.0'
  }
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${conversation.title}-${dayjs().format('YYYY-MM-DD')}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  
  ElMessage.success('对话已导出')
}

const handleDelete = async (id: string) => {
  const conversation = props.conversations.find(c => c.id === id)
  if (!conversation) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除对话"${conversation.title}"吗？此操作不可撤销。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    emit('delete', id)
    ElMessage.success('对话已删除')
    
  } catch {
    // 用户取消删除
  }
}
</script>

<style scoped>
.conversation-list {
  @apply h-full flex flex-col bg-white dark:bg-gray-800;
}

/* 头部区域 */
.list-header {
  @apply p-4 border-b border-gray-200 dark:border-gray-700;
}

.list-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3;
}

.create-button {
  @apply w-full;
}

/* 搜索区域 */
.search-container {
  @apply p-4 border-b border-gray-200 dark:border-gray-700;
}

/* 对话列表容器 */
.conversations-container {
  @apply flex-1 overflow-y-auto;
}

/* 空状态 */
.empty-state {
  @apply flex flex-col items-center justify-center h-full text-center p-8;
}

.empty-icon {
  @apply text-4xl mb-4 opacity-50;
}

.empty-text {
  @apply text-sm text-gray-500 dark:text-gray-400;
}

/* 对话列表 */
.conversations-list {
  @apply divide-y divide-gray-200 dark:divide-gray-700;
}

/* 对话项 */
.conversation-item {
  @apply flex items-center p-4 hover:bg-gray-50 dark:hover:bg-gray-700;
  @apply cursor-pointer transition-colors relative;
}

.conversation-item.active {
  @apply bg-blue-50 dark:bg-blue-900/20 border-r-2 border-blue-500;
}

.conversation-item.active::before {
  content: '';
  @apply absolute left-0 top-0 bottom-0 w-1 bg-blue-500;
}

.conversation-content {
  @apply flex-1 min-w-0;
}

.conversation-title {
  @apply mb-1;
}

.title-text {
  @apply font-medium text-gray-900 dark:text-gray-100 block truncate;
}

.conversation-meta {
  @apply flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400 mt-1;
}

.message-count {
  @apply bg-gray-100 dark:bg-gray-700 px-2 py-0.5 rounded-full;
}

.last-update {
  @apply font-mono;
}

/* 最后消息预览 */
.last-message {
  @apply text-sm text-gray-600 dark:text-gray-400 truncate;
}

.message-role {
  @apply font-medium text-gray-700 dark:text-gray-300;
}

.message-preview {
  @apply opacity-75;
}

/* 操作按钮 */
.conversation-actions {
  @apply flex-shrink-0 ml-2;
}

.action-button {
  @apply w-8 h-8 p-0 opacity-0 group-hover:opacity-100 transition-opacity;
}

.conversation-item:hover .action-button {
  @apply opacity-100;
}

/* 下拉菜单样式 */
:deep(.el-dropdown-menu__item) {
  @apply flex items-center gap-2;
}

:deep(.el-dropdown-menu__item.delete-item) {
  @apply text-red-600 dark:text-red-400;
}

:deep(.el-dropdown-menu__item.delete-item:hover) {
  @apply bg-red-50 dark:bg-red-900/20;
}

/* 对话框样式 */
:deep(.el-dialog) {
  @apply rounded-lg;
}

:deep(.el-dialog__header) {
  @apply border-b border-gray-200 dark:border-gray-700 pb-4;
}

:deep(.el-dialog__body) {
  @apply py-4;
}

/* 滚动条样式 */
.conversations-container::-webkit-scrollbar {
  @apply w-1;
}

.conversations-container::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.conversations-container::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}

.conversations-container::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400 dark:bg-gray-500;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .list-header {
    @apply p-3;
  }
  
  .search-container {
    @apply p-3;
  }
  
  .conversation-item {
    @apply p-3;
  }
  
  .list-title {
    @apply text-base mb-2;
  }
  
  .conversation-meta {
    @apply flex-col items-start gap-1;
  }
}

/* 动画效果 */
.conversation-item {
  transition: all 0.2s ease;
}

.conversation-item:hover {
  transform: translateX(2px);
}

.conversation-item.active {
  transform: translateX(0);
}

/* 无障碍设计 */
@media (prefers-reduced-motion: reduce) {
  .conversation-item {
    transition: none;
  }
  
  .conversation-item:hover {
    transform: none;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .conversation-item {
    @apply border border-gray-400 dark:border-gray-500 mb-1 rounded;
  }
  
  .conversation-item.active {
    @apply border-blue-600 dark:border-blue-400 border-2;
  }
}
</style> 