<template>
  <div class="test-view bg-white dark:bg-gray-900 min-h-screen p-8">
    <div class="max-w-4xl mx-auto">
      <h1 class="text-3xl font-bold text-center mb-8 text-gray-900 dark:text-white">
        🤖 AI Agent 测试页面
      </h1>
      
      <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 rounded-lg p-6 mb-6">
        <h2 class="text-xl font-semibold mb-4 text-blue-900 dark:text-blue-100">
          ✅ 基本功能测试
        </h2>
        <ul class="space-y-2 text-blue-800 dark:text-blue-200">
          <li>✅ Vue 3 渲染正常</li>
          <li>✅ TailwindCSS 样式工作</li>
          <li>✅ 深色模式支持</li>
          <li>✅ TypeScript 编译通过</li>
        </ul>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
          <h3 class="text-lg font-semibold mb-3 text-gray-900 dark:text-white">计数器测试</h3>
          <div class="flex items-center space-x-4">
            <button 
              @click="count--"
              class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded"
            >
              -
            </button>
            <span class="text-2xl font-bold text-gray-900 dark:text-white">{{ count }}</span>
            <button 
              @click="count++"
              class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded"
            >
              +
            </button>
          </div>
        </div>

        <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-6">
          <h3 class="text-lg font-semibold mb-3 text-gray-900 dark:text-white">主题切换</h3>
          <button 
            @click="toggleTheme"
            class="px-6 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded"
          >
            切换到{{ isDark ? '浅色' : '深色' }}模式
          </button>
        </div>
      </div>

      <div class="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700 rounded-lg p-6 mb-6">
        <h3 class="text-lg font-semibold mb-3 text-green-900 dark:text-green-100">
          🔗 路由测试
        </h3>
        <div class="space-x-4">
          <button 
            @click="goToChat"
            class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded"
          >
            前往聊天页面
          </button>
          <span class="text-green-800 dark:text-green-200">
            当前路由: {{ $route.path }}
          </span>
        </div>
      </div>

      <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg p-6">
        <h3 class="text-lg font-semibold mb-3 text-yellow-900 dark:text-yellow-100">
          📡 API 连接测试
        </h3>
        <div class="space-y-3">
          <button 
            @click="testAPI"
            :disabled="isLoading"
            class="px-4 py-2 bg-orange-500 hover:bg-orange-600 disabled:bg-gray-400 text-white rounded"
          >
            {{ isLoading ? '测试中...' : '测试后端连接' }}
          </button>
          <div v-if="apiResult" class="text-sm">
            <span class="font-semibold">结果:</span>
            <span :class="apiSuccess ? 'text-green-600' : 'text-red-600'">
              {{ apiResult }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 响应式数据
const count = ref(0)
const isDark = ref(false)
const isLoading = ref(false)
const apiResult = ref('')
const apiSuccess = ref(false)

// 方法
const toggleTheme = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

const goToChat = () => {
  router.push('/')
}

const testAPI = async () => {
  isLoading.value = true
  apiResult.value = ''
  
  try {
    // 测试后端健康检查
    const response = await fetch('/api/enhanced-agent/health')
    if (response.ok) {
      const result = await response.text()
      apiResult.value = result
      apiSuccess.value = true
    } else {
      apiResult.value = `HTTP ${response.status}: ${response.statusText}`
      apiSuccess.value = false
    }
  } catch (error) {
    apiResult.value = `连接失败: ${error}`
    apiSuccess.value = false
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.test-view {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}
</style> 