<template>
  <div class="chat-view" :class="{ 'dark': isDark }">
    <div class="chat-layout">
      <!-- 侧边栏 -->
      <div 
        class="sidebar" 
        :class="{ 'sidebar-collapsed': sidebarCollapsed }"
        v-show="!isMobile || showSidebar"
      >
        <ConversationList
          :conversations="conversations"
          :current-id="currentConversationId"
          @select="handleConversationSelect"
          @create="handleCreateConversation"
          @delete="handleDeleteConversation"
          @rename="handleRenameConversation"
        />
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 顶部导航栏 -->
        <div class="top-bar">
          <!-- 左侧操作 -->
          <div class="top-bar-left">
            <el-button
              link
              :icon="MenuIcon"
              @click="toggleSidebar"
              class="sidebar-toggle"
            />
            
            <h1 class="page-title">
              {{ currentConversationTitle }}
            </h1>
          </div>

          <!-- 右侧操作 -->
          <div class="top-bar-right">
            <!-- 连接状态指示器 -->
            <div class="connection-status" :class="connectionStatusClass">
              <div class="status-dot"></div>
              <span class="status-text">{{ connectionStatusText }}</span>
            </div>

            <!-- 设置按钮 -->
            <el-button
              link
              :icon="SettingIcon"
              @click="showSettings = true"
              title="设置"
            />

            <!-- 主题切换 -->
            <el-button
              link
              :icon="isDark ? SunIcon : MoonIcon"
              @click="toggleTheme"
              :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
            />
          </div>
        </div>

        <!-- 聊天窗口 -->
        <div class="chat-content">
          <ChatWindow
            :conversation-id="currentConversationId"
            :enable-typewriter="typewriterEnabled"
            :typewriter-speed="typewriterSpeed"
            @message-sent="handleMessageSent"
            @conversation-changed="handleConversationChanged"
          />
        </div>
      </div>
    </div>

    <!-- 设置对话框 -->
    <SettingsDialog
      v-model="showSettings"
      @update-config="handleConfigUpdate"
    />

    <!-- 移动端遮罩 -->
    <div
      v-if="isMobile && showSidebar"
      class="mobile-overlay"
      @click="closeSidebar"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import { useConfigStore } from '@/stores/config'
import { useWindowSize } from '@vueuse/core'

import ChatWindow from '@/components/chat/ChatWindow.vue'
import ConversationList from '@/components/chat/ConversationList.vue'
import SettingsDialog from '@/components/common/SettingsDialog.vue'

// 图标
import {
  Menu as MenuIcon,
  Setting as SettingIcon,
  Sunny as SunIcon,
  Moon as MoonIcon
} from '@element-plus/icons-vue'

// Store
const chatStore = useChatStore()
const configStore = useConfigStore()
const router = useRouter()

// 响应式数据
const showSettings = ref(false)
const sidebarCollapsed = ref(false)
const showSidebar = ref(false)

// 屏幕尺寸检测
const { width } = useWindowSize()
const isMobile = computed(() => width.value < 768)

// 计算属性
const conversations = computed(() => chatStore.conversationList)
const currentConversationId = computed(() => chatStore.currentConversationId)
const isConnecting = computed(() => chatStore.isConnecting)
const isDark = computed(() => configStore.theme.mode === 'dark')
const typewriterEnabled = computed(() => configStore.agent.typewriter.enabled)
const typewriterSpeed = computed(() => configStore.agent.typewriter.delay)

const currentConversationTitle = computed(() => {
  const current = chatStore.currentConversation
  return current?.title || 'AI Agent Chat'
})

const connectionStatusClass = computed(() => ({
  'status-connected': !isConnecting.value,
  'status-connecting': isConnecting.value
}))

const connectionStatusText = computed(() => {
  return isConnecting.value ? '连接中' : '已连接'
})

// 方法
const toggleSidebar = () => {
  if (isMobile.value) {
    showSidebar.value = !showSidebar.value
  } else {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
}

const closeSidebar = () => {
  showSidebar.value = false
}

const toggleTheme = () => {
  configStore.toggleTheme()
  ElMessage.success(`已切换到${isDark.value ? '深色' : '浅色'}模式`)
}

const handleConversationSelect = (id: string) => {
  chatStore.switchConversation(id)
  closeSidebar()
  router.push({ query: { conversation: id } })
}

const handleCreateConversation = () => {
  const id = chatStore.createConversation()
  router.push({ query: { conversation: id } })
  closeSidebar()
  ElMessage.success('新建对话成功')
}

const handleDeleteConversation = (id: string) => {
  chatStore.deleteConversation(id)
  
  // 如果删除的是当前对话，重定向到首页
  if (id === currentConversationId.value) {
    router.push({ query: {} })
  }
  
  ElMessage.success('删除对话成功')
}

const handleRenameConversation = (id: string, newTitle: string) => {
  const conversation = chatStore.conversations.get(id)
  if (conversation) {
    conversation.title = newTitle
    conversation.updatedAt = Date.now()
    ElMessage.success('重命名成功')
  }
}

const handleMessageSent = (message: string) => {
  console.log('Message sent:', message)
}

const handleConversationChanged = (id: string) => {
  router.push({ query: { conversation: id } })
}

const handleConfigUpdate = (config: any) => {
  ElMessage.success('配置已更新')
}

// 键盘快捷键
const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl/Cmd + N: 新建对话
  if ((event.ctrlKey || event.metaKey) && event.key === 'n') {
    event.preventDefault()
    handleCreateConversation()
  }
  
  // Ctrl/Cmd + /: 切换侧边栏
  if ((event.ctrlKey || event.metaKey) && event.key === '/') {
    event.preventDefault()
    toggleSidebar()
  }
  
  // Ctrl/Cmd + ,: 打开设置
  if ((event.ctrlKey || event.metaKey) && event.key === ',') {
    event.preventDefault()
    showSettings.value = true
  }
  
  // Ctrl/Cmd + D: 切换主题
  if ((event.ctrlKey || event.metaKey) && event.key === 'd') {
    event.preventDefault()
    toggleTheme()
  }
}

// 生命周期
onMounted(() => {
  // 从URL参数恢复对话
  const conversationId = router.currentRoute.value.query.conversation as string
  if (conversationId) {
    chatStore.switchConversation(conversationId)
  } else if (conversations.value.length === 0) {
    // 如果没有对话，创建第一个对话
    handleCreateConversation()
  }
  
  // 监听键盘事件
  document.addEventListener('keydown', handleKeydown)
  
  // 移动端自动隐藏侧边栏
  if (isMobile.value) {
    showSidebar.value = false
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.chat-view {
  @apply h-screen bg-gray-50 dark:bg-gray-900 overflow-hidden;
}

.chat-layout {
  @apply h-full flex;
}

/* 侧边栏 */
.sidebar {
  @apply w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700;
  @apply flex-shrink-0 transition-all duration-300;
}

.sidebar-collapsed {
  @apply w-16;
}

@media (max-width: 767px) {
  .sidebar {
    @apply fixed inset-y-0 left-0 z-40 w-80;
    @apply transform transition-transform duration-300;
  }
  
  .sidebar:not(.show) {
    @apply -translate-x-full;
  }
}

/* 主内容区 */
.main-content {
  @apply flex-1 flex flex-col min-w-0;
}

/* 顶部导航栏 */
.top-bar {
  @apply h-16 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700;
  @apply flex items-center justify-between px-4;
  @apply shadow-sm;
}

.top-bar-left {
  @apply flex items-center gap-3;
}

.top-bar-right {
  @apply flex items-center gap-2;
}

.sidebar-toggle {
  @apply p-2;
}

.page-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
  @apply truncate max-w-xs;
}

/* 连接状态指示器 */
.connection-status {
  @apply flex items-center gap-2 px-3 py-1 rounded-full text-sm;
  @apply transition-colors;
}

.status-connected {
  @apply bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-300;
}

.status-connecting {
  @apply bg-orange-100 dark:bg-orange-900/20 text-orange-700 dark:text-orange-300;
}

.status-dot {
  @apply w-2 h-2 rounded-full;
}

.status-connected .status-dot {
  @apply bg-green-500;
}

.status-connecting .status-dot {
  @apply bg-orange-500;
  animation: pulse 2s infinite;
}

/* 聊天内容区 */
.chat-content {
  @apply flex-1 min-h-0;
}

/* 移动端遮罩 */
.mobile-overlay {
  @apply fixed inset-0 bg-black/50 z-30;
  @apply md:hidden;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .top-bar {
    @apply px-3 h-14;
  }
  
  .page-title {
    @apply text-base max-w-48;
  }
  
  .connection-status {
    @apply hidden;
  }
}

/* 动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 滚动条样式 */
.sidebar::-webkit-scrollbar {
  @apply w-1;
}

.sidebar::-webkit-scrollbar-track {
  @apply bg-transparent;
}

.sidebar::-webkit-scrollbar-thumb {
  @apply bg-gray-300 dark:bg-gray-600 rounded-full;
}
</style> 