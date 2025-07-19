import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { nanoid } from 'nanoid'
import type { ChatMessage, Conversation } from '@/api/types'
import { agentAPI } from '@/api/agent'

export const useChatStore = defineStore('chat', () => {
  // 状态定义
  const conversations = ref(new Map<string, Conversation>())
  const currentConversationId = ref<string>('')
  const isLoading = ref(false)
  const isConnecting = ref(false)
  const error = ref<string | null>(null)
  const eventSource = ref<EventSource | null>(null)

  // 计算属性
  const currentConversation = computed(() => 
    conversations.value.get(currentConversationId.value)
  )

  const currentMessages = computed(() => 
    currentConversation.value?.messages || []
  )

  const conversationList = computed(() => 
    Array.from(conversations.value.values())
      .sort((a, b) => b.updatedAt - a.updatedAt)
  )

  // 辅助函数
  const generateConversationTitle = (firstMessage: string): string => {
    const title = firstMessage.slice(0, 30)
    return title.length < firstMessage.length ? title + '...' : title
  }

  // 创建新对话
  const createConversation = (title?: string): string => {
    const id = nanoid()
    const now = Date.now()
    
    const conversation: Conversation = {
      id,
      title: title || '新对话',
      messages: [],
      createdAt: now,
      updatedAt: now
    }
    
    conversations.value.set(id, conversation)
    currentConversationId.value = id
    
    console.log('📝 Created new conversation:', id)
    return id
  }

  // 切换对话
  const switchConversation = (id: string): void => {
    if (conversations.value.has(id)) {
      currentConversationId.value = id
      console.log('🔄 Switched to conversation:', id)
    }
  }

  // 删除对话
  const deleteConversation = (id: string): void => {
    conversations.value.delete(id)
    
    if (currentConversationId.value === id) {
      const remaining = conversationList.value
      currentConversationId.value = remaining.length > 0 ? remaining[0].id : ''
    }
    
    console.log('🗑️ Deleted conversation:', id)
  }

  // 添加消息
  const addMessage = (message: Omit<ChatMessage, 'id' | 'timestamp'>): ChatMessage => {
    const fullMessage: ChatMessage = {
      ...message,
      id: nanoid(),
      timestamp: Date.now()
    }

    const conversation = conversations.value.get(message.conversationId)
    if (conversation) {
      conversation.messages.push(fullMessage)
      conversation.updatedAt = fullMessage.timestamp
      
      // 如果是第一条用户消息，更新对话标题
      if (conversation.messages.length === 1 && fullMessage.role === 'user') {
        conversation.title = generateConversationTitle(fullMessage.content)
      }
    }

    return fullMessage
  }

  // 添加工具思考消息
  const addToolThinkingMessage = (conversationId: string, content: string): ChatMessage => {
    return addMessage({
      conversationId,
      role: 'tool_thinking',
      content,
      messageType: 'tool_thinking',
      isComplete: true
    })
  }

  // 更新消息内容（用于流式更新）
  const updateMessage = (messageId: string, content: string, isComplete = false): void => {
    for (const conversation of conversations.value.values()) {
      const message = conversation.messages.find(m => m.id === messageId)
      if (message) {
        message.content = content
        message.isComplete = isComplete
        message.isStreaming = !isComplete
        conversation.updatedAt = Date.now()
        break
      }
    }
  }

  // 发送普通消息
  const sendMessage = async (content: string, conversationId?: string): Promise<void> => {
    try {
      isLoading.value = true
      error.value = null

      // 确保有对话ID
      const targetConversationId = conversationId || currentConversationId.value || createConversation()

      // 添加用户消息
      addMessage({
        conversationId: targetConversationId,
        role: 'user',
        content,
        isComplete: true
      })

      // 发送API请求
      const response = await agentAPI.chat({
        message: content,
        conversationId: targetConversationId
      })

      // 添加AI回复
      addMessage({
        conversationId: targetConversationId,
        role: 'assistant',
        content: response.result,
        isComplete: true
      })

    } catch (err) {
      console.error('❌ Send message error:', err)
      error.value = err instanceof Error ? err.message : '发送消息失败'
    } finally {
      isLoading.value = false
    }
  }

  // 发送流式消息
  const sendStreamMessage = (content: string, conversationId?: string): void => {
    try {
      // 重置状态
      isConnecting.value = true
      error.value = null

      // 确保有对话ID
      const targetConversationId = conversationId || currentConversationId.value || createConversation()

      // 添加用户消息
      addMessage({
        conversationId: targetConversationId,
        role: 'user',
        content,
        isComplete: true
      })

      // 创建助手消息占位符
      const assistantMessage = addMessage({
        conversationId: targetConversationId,
        role: 'assistant',
        content: '',
        isStreaming: true,
        isComplete: false
      })

      // 关闭之前的连接
      if (eventSource.value) {
        eventSource.value.close()
      }

      // 创建新的SSE连接
      eventSource.value = agentAPI.createStreamChat({
        message: content,
        conversationId: targetConversationId
      })

      let accumulatedContent = ''

      eventSource.value.onmessage = (event) => {
        const data = event.data
        if (data && data !== '[DONE]') {
          accumulatedContent += data
          updateMessage(assistantMessage.id, accumulatedContent, false)
        }
      }

      // 监听工具调用思考过程事件
      eventSource.value.addEventListener('tool_thinking', (event: any) => {
        const thinkingData = event.data
        if (thinkingData) {
          // 添加工具思考消息
          addToolThinkingMessage(targetConversationId, thinkingData)
        }
      })

      eventSource.value.onopen = () => {
        isConnecting.value = false
        console.log('🌊 SSE connection opened')
      }

      eventSource.value.onerror = (err) => {
        console.error('❌ SSE error:', err)
        const wasConnecting = isConnecting.value
        isConnecting.value = false
        updateMessage(assistantMessage.id, accumulatedContent || '连接错误，请重试', true)
        // 只在之前还在连接状态下才设置错误信息，避免正常结束时显示错误
        if (wasConnecting) {
          error.value = '连接中断，请重试'
        }
        eventSource.value?.close()
      }

      // 监听后端发送的正常关闭事件
      eventSource.value.addEventListener('close', (event: any) => {
        isConnecting.value = false
        updateMessage(assistantMessage.id, accumulatedContent, true)
        console.log('✅ SSE connection closed normally by server')
        error.value = null // 清除错误状态
        eventSource.value?.close()
      })

    } catch (err) {
      console.error('❌ Stream message error:', err)
      isConnecting.value = false
      error.value = err instanceof Error ? err.message : '发送消息失败'
    }
  }

  // 停止流式传输
  const stopStreaming = (): void => {
    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
      isConnecting.value = false
      console.log('⏹️ Streaming stopped')
    }
  }

  // 清空所有对话
  const clearAllConversations = (): void => {
    conversations.value.clear()
    currentConversationId.value = ''
    console.log('🧹 All conversations cleared')
  }

  // 导出对话数据
  const exportConversations = () => {
    const data = {
      conversations: conversationList.value,
      exportTime: Date.now(),
      version: '1.0.0'
    }
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ai-chat-export-${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    
    console.log('📤 Conversations exported')
  }

  return {
    // 状态
    conversations,
    currentConversationId,
    isLoading,
    isConnecting,
    error,
    
    // 计算属性
    currentConversation,
    currentMessages,
    conversationList,
    
    // 方法
    createConversation,
    switchConversation,
    deleteConversation,
    addMessage,
    addToolThinkingMessage,
    updateMessage,
    sendMessage,
    sendStreamMessage,
    stopStreaming,
    clearAllConversations,
    exportConversations
  }
}) 