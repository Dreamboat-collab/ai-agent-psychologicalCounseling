import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ChatRequest, ChatResponse, TypewriterConfig } from './types'

// 创建axios实例
const api = axios.create({
  baseURL: '/api/enhanced-agent',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    console.log('🚀 API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    console.error('❌ Request Error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('❌ Response Error:', error.response?.status, error.message)
    
    // 统一错误处理
    if (error.response?.status === 500) {
      ElMessage.error('服务器内部错误，请稍后重试')
    } else if (error.response?.status === 404) {
      ElMessage.error('请求的资源不存在')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    
    return Promise.reject(error)
  }
)

export const agentAPI = {
  /**
   * 普通聊天接口
   */
  async chat(request: ChatRequest): Promise<ChatResponse> {
    const formData = new FormData()
    formData.append('message', request.message)
    if (request.conversationId) {
      formData.append('conversationId', request.conversationId)
    }

    const response = await api.post('/chat', formData, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    })
    
    return {
      result: response.data,
      conversationId: request.conversationId || ''
    }
  },

  /**
   * 流式聊天接口 - 创建EventSource连接
   */
  createStreamChat(request: ChatRequest): EventSource {
    const params = new URLSearchParams()
    params.append('message', request.message)
    if (request.conversationId) {
      params.append('conversationId', request.conversationId)
    }

    const url = `/api/enhanced-agent/chat-stream?${params.toString()}`
    console.log('🌊 Creating SSE connection:', url)
    
    return new EventSource(url)
  },

  /**
   * 配置打字机效果
   */
  async configTypewriter(config: TypewriterConfig): Promise<void> {
    const formData = new FormData()
    formData.append('enable', config.enabled.toString())
    formData.append('delay', config.delay.toString())

    await api.post('/config/typewriter', formData, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    })
  },

  /**
   * 健康检查
   */
  async health(): Promise<string> {
    const response = await api.get('/health')
    return response.data
  }
}

export default api 