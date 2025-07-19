import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'

// 创建应用实例
const app = createApp(App)

// Pinia状态管理
const pinia = createPinia()

// 配置 Pinia - 禁用有问题的 DevTools 集成
if (import.meta.env.DEV) {
  console.info('💡 Development mode: DevTools integration disabled for stability')
}

app.use(pinia)

// Vue Router
app.use(router)

// Element Plus UI组件库
app.use(ElementPlus)

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  // 检查是否是 DevTools 相关错误（可以安全忽略）
  if (err instanceof TypeError && err.message.includes('hook.emit is not a function')) {
    console.warn('🔧 DevTools hook error (safe to ignore):', err.message)
    return // 忽略这类错误，不影响应用运行
  }
  
  console.error('🚨 Global Error:', err)
  console.error('🎯 Error Info:', info)
  console.error('🔍 Component:', vm)
  
  // 可以在这里添加错误上报逻辑
  // 比如发送到错误监控服务
}

// 全局警告处理
app.config.warnHandler = (msg, vm, trace) => {
  console.warn('⚠️ Warning:', msg)
  console.warn('🔍 Trace:', trace)
}

// 全局属性
app.config.globalProperties.$appVersion = '1.0.0'
app.config.globalProperties.$appName = 'AI Agent Chat'

// 开发环境配置
if (import.meta.env.DEV) {
  console.log('🔧 Development mode enabled')
  
  // 开发工具
  app.config.performance = true
  
  // Vue DevTools
  if (typeof window !== 'undefined') {
    (window as any).__VUE_DEVTOOLS_GLOBAL_HOOK__ = (window as any).__VUE_DEVTOOLS_GLOBAL_HOOK__ || {}
  }
}

// 生产环境配置
if (import.meta.env.PROD) {
  console.log('🚀 Production mode')
  
  // 禁用开发工具提示
  // app.config.devtools = false // 注释掉，避免 TypeScript 错误
  
  // 移除控制台日志（可选）
  // console.log = console.warn = console.error = () => {}
}

// 初始化配置
import { useConfigStore } from '@/stores/config'

// 应用挂载前的准备工作
const initializeApp = async () => {
  try {
    // 显示加载状态
    const loadingElement = document.getElementById('loading')
    if (loadingElement) {
      loadingElement.style.display = 'flex'
    }

    // 初始化配置
    const configStore = useConfigStore()
    configStore.initialize()

    // 检查后端服务连接状态（异步，不阻塞页面加载）
    setTimeout(async () => {
      try {
        const { agentAPI } = await import('@/api/agent')
        await agentAPI.health()
        console.log('✅ Backend service connected')
      } catch (error) {
        console.warn('⚠️ Backend service not available:', error)
        // 可以显示离线模式提示
      }
    }, 1000)

    // 隐藏加载状态
    if (loadingElement) {
      setTimeout(() => {
        loadingElement.style.display = 'none'
      }, 500)
    }

    console.log('🎉 Application initialized successfully')
    
  } catch (error) {
    console.error('❌ Application initialization failed:', error)
  }
}

// 挂载应用
app.mount('#app')

// 初始化
initializeApp()

// 全局键盘快捷键
document.addEventListener('keydown', (event) => {
  // F12 或 Ctrl+Shift+I: 开发者工具（仅开发环境）
  if (import.meta.env.DEV) {
    if (event.key === 'F12' || (event.ctrlKey && event.shiftKey && event.key === 'I')) {
      console.log('🔧 Developer tools shortcut')
    }
  }
})

// PWA支持准备
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', async () => {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js')
      console.log('✅ Service Worker registered:', registration)
    } catch (error) {
      console.error('❌ Service Worker registration failed:', error)
    }
  })
}

// 性能监控
if (import.meta.env.PROD) {
  // 监控首屏加载时间
  window.addEventListener('load', () => {
    const loadTime = performance.now()
    console.log(`📊 Page load time: ${loadTime.toFixed(2)}ms`)
    
    // 可以发送到性能监控服务
  })
  
  // 监控导航性能
  if ('navigation' in performance) {
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
    console.log('📊 Navigation timing:', {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
      totalTime: navigation.loadEventEnd - navigation.fetchStart
    })
  }
}

// 内存泄漏检测（开发环境）
if (import.meta.env.DEV) {
  let memoryCheckInterval: number
  
  const checkMemory = () => {
    if ('memory' in performance) {
      const memory = (performance as any).memory
      console.log('💾 Memory usage:', {
        used: `${Math.round(memory.usedJSHeapSize / 1024 / 1024)}MB`,
        total: `${Math.round(memory.totalJSHeapSize / 1024 / 1024)}MB`,
        limit: `${Math.round(memory.jsHeapSizeLimit / 1024 / 1024)}MB`
      })
    }
  }
  
  // 每30秒检查一次内存使用情况
  memoryCheckInterval = window.setInterval(checkMemory, 30000)
  
  // 清理
  window.addEventListener('beforeunload', () => {
    if (memoryCheckInterval) {
      clearInterval(memoryCheckInterval)
    }
  })
}

// 导出应用实例（用于测试）
export default app 