// 消息类型
export interface ChatMessage {
  id: string
  conversationId: string
  role: 'user' | 'assistant' | 'system' | 'tool_thinking'
  content: string
  timestamp: number
  isStreaming?: boolean
  isComplete?: boolean
  messageType?: 'normal' | 'tool_thinking' // 消息子类型
}

// 对话会话
export interface Conversation {
  id: string
  title: string
  messages: ChatMessage[]
  createdAt: number
  updatedAt: number
}

// API请求/响应类型
export interface ChatRequest {
  message: string
  conversationId?: string
}

export interface ChatResponse {
  result: string
  conversationId: string
}

// 打字机配置
export interface TypewriterConfig {
  enabled: boolean
  delay: number
}

// Agent配置
export interface AgentConfig {
  typewriter: TypewriterConfig
  maxMessages: number
  autoSave: boolean
  soundEnabled: boolean
}

// 主题配置
export interface ThemeConfig {
  mode: 'light' | 'dark'
  primaryColor: string
}

// 应用状态
export interface AppState {
  loading: boolean
  connecting: boolean
  error: string | null
}

// SSE事件类型
export interface SSEEvent {
  data: string
  type: 'message' | 'error' | 'complete'
}

// 导出聊天数据格式
export interface ExportData {
  conversations: Conversation[]
  exportTime: number
  version: string
} 