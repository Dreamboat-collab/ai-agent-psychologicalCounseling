<template>
  <div id="app" class="app-container" :class="appClasses">
    <!-- 全局加载指示器 -->
    <div id="loading" class="loading-overlay" v-if="isInitializing">
      <div class="loading-content">
        <div class="loading-spinner"></div>
        <p class="loading-text">AI Agent 正在启动...</p>
      </div>
    </div>

    <!-- 主路由视图 -->
    <router-view v-slot="{ Component, route }">
      <transition 
        :name="transitionName" 
        mode="out-in"
        @before-enter="onBeforeEnter"
        @after-enter="onAfterEnter"
      >
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>

    <!-- 全局通知容器 -->
    <Teleport to="body">
      <div id="notification-container" class="notification-container"></div>
    </Teleport>

    <!-- 全局模态框容器 -->
    <Teleport to="body">
      <div id="modal-container" class="modal-container"></div>
    </Teleport>

    <!-- 快捷键帮助面板 -->
    <div 
      v-if="showKeyboardHelp" 
      class="keyboard-help-overlay"
      @click="hideKeyboardHelp"
    >
      <div class="keyboard-help-panel" @click.stop>
        <div class="help-header">
          <h3>快捷键</h3>
          <button @click="hideKeyboardHelp" class="close-btn">×</button>
        </div>
        <div class="help-content">
          <div class="help-section">
            <h4>全局快捷键</h4>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>N</kbd>
              <span>新建对话</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>/</kbd>
              <span>切换侧边栏</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>,</kbd>
              <span>打开设置</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>D</kbd>
              <span>切换主题</span>
            </div>
          </div>
          <div class="help-section">
            <h4>聊天快捷键</h4>
            <div class="shortcut-item">
              <kbd>Enter</kbd>
              <span>发送消息</span>
            </div>
            <div class="shortcut-item">
              <kbd>Shift</kbd> + <kbd>Enter</kbd>
              <span>换行</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>Enter</kbd>
              <span>强制发送</span>
            </div>
            <div class="shortcut-item">
              <kbd>Escape</kbd>
              <span>清空输入</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useConfigStore } from '@/stores/config'

// Store
const configStore = useConfigStore()
const route = useRoute()

// 响应式数据
const isInitializing = ref(true)
const showKeyboardHelp = ref(false)
const isPageTransitioning = ref(false)

// 计算属性
const appClasses = computed(() => ({
  'dark': configStore.isDark,
  'transitioning': isPageTransitioning.value,
  'compact': configStore.app.compactMode,
  [`font-${configStore.app.fontSize}`]: true
}))

const transitionName = computed(() => {
  // 根据路由变化决定过渡动画
  const from = route.meta?.from as string
  const to = route.name as string
  
  if (from === 'Settings' || to === 'Settings') {
    return 'slide-up'
  } else if (from === 'Chat' && to === 'Chat') {
    return 'fade'
  } else {
    return 'slide-right'
  }
})

// 方法
const onBeforeEnter = () => {
  isPageTransitioning.value = true
}

const onAfterEnter = () => {
  isPageTransitioning.value = false
}

const showHelp = () => {
  showKeyboardHelp.value = true
}

const hideKeyboardHelp = () => {
  showKeyboardHelp.value = false
}

// 全局键盘事件处理
const handleGlobalKeydown = (event: KeyboardEvent) => {
  // ? 或 F1: 显示帮助
  if (event.key === '?' || event.key === 'F1') {
    event.preventDefault()
    showKeyboardHelp.value = !showKeyboardHelp.value
  }
  
  // Escape: 隐藏帮助面板
  if (event.key === 'Escape' && showKeyboardHelp.value) {
    event.preventDefault()
    hideKeyboardHelp()
  }
}

// 生命周期
onMounted(async () => {
  // 应用初始化
  try {
    console.log('🚀 App mounted, initializing...')
    
    // 初始化配置
    configStore.initialize()
    
    // 快速初始化，减少加载时间
    await new Promise(resolve => setTimeout(resolve, 300))
    
    isInitializing.value = false
    console.log('✅ App initialization completed')
    
  } catch (error) {
    console.error('❌ App initialization failed:', error)
    isInitializing.value = false
  }
  
  // 添加全局键盘事件监听
  document.addEventListener('keydown', handleGlobalKeydown)
  
  // 添加全局CSS变量
  const root = document.documentElement
  Object.entries(configStore.cssVariables).forEach(([key, value]) => {
    root.style.setProperty(key, value)
  })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
})

// 错误边界处理
const onError = (error: Error) => {
  console.error('🚨 Component Error:', error)
  // 可以显示错误提示或者错误页面
}
</script>

<style scoped>
.app-container {
  @apply w-full h-screen overflow-hidden;
  @apply bg-gray-50 dark:bg-gray-900;
  @apply text-gray-900 dark:text-gray-100;
  @apply transition-colors duration-300;
}

/* 加载覆盖层 */
.loading-overlay {
  @apply fixed inset-0 z-50;
  @apply bg-white dark:bg-gray-900;
  @apply flex items-center justify-center;
}

.loading-content {
  @apply text-center;
}

.loading-spinner {
  @apply w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4;
  animation: spin 1s linear infinite;
}

.loading-text {
  @apply text-lg text-gray-600 dark:text-gray-400;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-right-enter-from {
  transform: translateX(20px);
  opacity: 0;
}

.slide-right-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(20px);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

/* 通知和模态框容器 */
.notification-container,
.modal-container {
  @apply pointer-events-none;
}

/* 快捷键帮助面板 */
.keyboard-help-overlay {
  @apply fixed inset-0 z-50 bg-black/50;
  @apply flex items-center justify-center p-4;
}

.keyboard-help-panel {
  @apply bg-white dark:bg-gray-800 rounded-lg shadow-xl;
  @apply max-w-md w-full max-h-96 overflow-y-auto;
}

.help-header {
  @apply flex items-center justify-between p-4 border-b border-gray-200 dark:border-gray-700;
}

.help-header h3 {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100;
}

.close-btn {
  @apply w-8 h-8 flex items-center justify-center;
  @apply text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200;
  @apply rounded-full hover:bg-gray-100 dark:hover:bg-gray-700;
  @apply transition-colors text-xl font-light;
}

.help-content {
  @apply p-4 space-y-4;
}

.help-section h4 {
  @apply font-medium text-gray-900 dark:text-gray-100 mb-2;
}

.shortcut-item {
  @apply flex items-center justify-between py-1;
}

.shortcut-item kbd {
  @apply inline-block px-2 py-1 text-xs font-mono;
  @apply bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600;
  @apply rounded shadow-sm;
}

.shortcut-item span {
  @apply text-sm text-gray-600 dark:text-gray-400;
}

/* 字体大小变量 */
.font-small {
  font-size: calc(1rem * 0.875);
}

.font-medium {
  font-size: 1rem;
}

.font-large {
  font-size: calc(1rem * 1.125);
}

/* 紧凑模式 */
.compact .el-button {
  @apply px-3 py-1 text-sm;
}

.compact .el-input {
  @apply text-sm;
}

/* 过渡状态样式 */
.transitioning {
  @apply pointer-events-none;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .keyboard-help-panel {
    @apply mx-4;
  }
  
  .loading-text {
    @apply text-base;
  }
}

/* 无障碍设计 */
@media (prefers-reduced-motion: reduce) {
  .fade-enter-active,
  .fade-leave-active,
  .slide-right-enter-active,
  .slide-right-leave-active,
  .slide-up-enter-active,
  .slide-up-leave-active {
    transition: none;
  }
  
  .loading-spinner {
    animation: none;
  }
}

/* 高对比度模式 */
@media (prefers-contrast: high) {
  .app-container {
    @apply border border-gray-800 dark:border-gray-200;
  }
  
  .shortcut-item kbd {
    @apply border-2 border-gray-800 dark:border-gray-200;
  }
}
</style> 