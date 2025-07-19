import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { ThemeConfig, AgentConfig } from '@/api/types'
import { agentAPI } from '@/api/agent'

export const useConfigStore = defineStore('config', () => {
  // 主题配置
  const theme = ref<ThemeConfig>({
    mode: 'light',
    primaryColor: '#3b82f6'
  })

  // Agent配置
  const agent = ref<AgentConfig>({
    typewriter: {
      enabled: true,
      delay: 50
    },
    maxMessages: 100,
    autoSave: true,
    soundEnabled: false
  })

  // 应用设置
  const app = ref({
    language: 'zh-CN',
    autoScroll: true,
    showTimestamp: true,
    compactMode: false,
    sidebarWidth: 320,
    fontSize: 'medium' as 'small' | 'medium' | 'large'
  })

  // 计算属性
  const isDark = computed(() => theme.value.mode === 'dark')
  
  const cssVariables = computed(() => ({
    '--primary-color': theme.value.primaryColor,
    '--sidebar-width': `${app.value.sidebarWidth}px`,
    '--font-size-multiplier': app.value.fontSize === 'small' ? '0.875' : 
                             app.value.fontSize === 'large' ? '1.125' : '1'
  }))

  // 方法
  const toggleTheme = () => {
    theme.value.mode = theme.value.mode === 'light' ? 'dark' : 'light'
    applyTheme()
  }

  const setTheme = (mode: 'light' | 'dark') => {
    theme.value.mode = mode
    applyTheme()
  }

  const applyTheme = () => {
    const root = document.documentElement
    
    if (theme.value.mode === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
    
    // 应用CSS变量
    Object.entries(cssVariables.value).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })
  }

  const updateTypewriterConfig = async (config: { enabled: boolean; delay: number }) => {
    try {
      await agentAPI.configTypewriter(config)
      agent.value.typewriter = { ...config }
      console.log('✅ Typewriter config updated')
    } catch (error) {
      console.error('❌ Failed to update typewriter config:', error)
      throw error
    }
  }

  const resetToDefaults = () => {
    theme.value = {
      mode: 'light',
      primaryColor: '#3b82f6'
    }
    
    agent.value = {
      typewriter: {
        enabled: true,
        delay: 50
      },
      maxMessages: 100,
      autoSave: true,
      soundEnabled: false
    }
    
    app.value = {
      language: 'zh-CN',
      autoScroll: true,
      showTimestamp: true,
      compactMode: false,
      sidebarWidth: 320,
      fontSize: 'medium'
    }
    
    applyTheme()
    saveToStorage()
  }

  const exportConfig = () => {
    const config = {
      theme: theme.value,
      agent: agent.value,
      app: app.value,
      exportTime: Date.now(),
      version: '1.0.0'
    }
    
    const blob = new Blob([JSON.stringify(config, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ai-agent-config-${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const importConfig = (configData: any) => {
    try {
      if (configData.theme) {
        theme.value = { ...theme.value, ...configData.theme }
      }
      
      if (configData.agent) {
        agent.value = { ...agent.value, ...configData.agent }
      }
      
      if (configData.app) {
        app.value = { ...app.value, ...configData.app }
      }
      
      applyTheme()
      saveToStorage()
      
      return true
    } catch (error) {
      console.error('Failed to import config:', error)
      return false
    }
  }

  // 本地存储
  const STORAGE_KEY = 'ai-agent-config'
  
  const saveToStorage = () => {
    try {
      const config = {
        theme: theme.value,
        agent: agent.value,
        app: app.value
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
    } catch (error) {
      console.error('Failed to save config to storage:', error)
    }
  }

  const loadFromStorage = () => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const config = JSON.parse(stored)
        
        if (config.theme) {
          theme.value = { ...theme.value, ...config.theme }
        }
        
        if (config.agent) {
          agent.value = { ...agent.value, ...config.agent }
        }
        
        if (config.app) {
          app.value = { ...app.value, ...config.app }
        }
        
        console.log('✅ Config loaded from storage')
      }
    } catch (error) {
      console.error('Failed to load config from storage:', error)
    }
  }

  // 监听器 - 自动保存
  watch([theme, agent, app], () => {
    saveToStorage()
  }, { deep: true })

  // 监听主题变化
  watch(() => theme.value.mode, () => {
    applyTheme()
  })

  // 监听系统主题变化
  const watchSystemTheme = () => {
    if (typeof window !== 'undefined') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      
      const handleChange = (e: MediaQueryListEvent) => {
        if (theme.value.mode === 'auto') {
          applyTheme()
        }
      }
      
      mediaQuery.addEventListener('change', handleChange)
      
      return () => mediaQuery.removeEventListener('change', handleChange)
    }
  }

  // 预设主题
  const themePresets = [
    { name: '蓝色', color: '#3b82f6' },
    { name: '绿色', color: '#10b981' },
    { name: '紫色', color: '#8b5cf6' },
    { name: '粉色', color: '#ec4899' },
    { name: '橙色', color: '#f59e0b' },
    { name: '红色', color: '#ef4444' }
  ]

  const applyThemePreset = (color: string) => {
    theme.value.primaryColor = color
    applyTheme()
  }

  // 字体大小设置
  const fontSizeOptions = [
    { label: '小', value: 'small' as const },
    { label: '中', value: 'medium' as const },
    { label: '大', value: 'large' as const }
  ]

  const setFontSize = (size: 'small' | 'medium' | 'large') => {
    app.value.fontSize = size
    applyTheme()
  }

  // 初始化
  const initialize = () => {
    loadFromStorage()
    applyTheme()
    
    // 监听系统主题变化
    const cleanup = watchSystemTheme()
    
    return cleanup
  }

  return {
    // 状态
    theme,
    agent,
    app,
    
    // 计算属性
    isDark,
    cssVariables,
    themePresets,
    fontSizeOptions,
    
    // 方法
    toggleTheme,
    setTheme,
    applyTheme,
    updateTypewriterConfig,
    resetToDefaults,
    exportConfig,
    importConfig,
    saveToStorage,
    loadFromStorage,
    applyThemePreset,
    setFontSize,
    initialize
  }
}) 