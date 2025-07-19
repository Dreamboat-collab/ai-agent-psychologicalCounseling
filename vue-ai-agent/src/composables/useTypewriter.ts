import { ref, computed, watch, onUnmounted } from 'vue'

export interface TypewriterOptions {
  speed?: number
  autoStart?: boolean
  showCursor?: boolean
  cursorChar?: string
  loop?: boolean
}

export function useTypewriter(
  text: string | Ref<string>,
  options: TypewriterOptions = {}
) {
  const {
    speed = 50,
    autoStart = true,
    showCursor = true,
    cursorChar = '|',
    loop = false
  } = options

  // 响应式状态
  const displayText = ref('')
  const isTyping = ref(false)
  const isComplete = ref(false)
  const currentIndex = ref(0)

  // 定时器引用
  let timer: NodeJS.Timeout | null = null

  // 获取当前文本内容
  const currentText = computed(() => 
    typeof text === 'string' ? text : text.value
  )

  // 光标显示状态
  const cursorVisible = ref(true)
  let cursorTimer: NodeJS.Timeout | null = null

  // 光标闪烁
  const startCursorBlink = () => {
    if (!showCursor) return
    
    cursorTimer = setInterval(() => {
      cursorVisible.value = !cursorVisible.value
    }, 500)
  }

  const stopCursorBlink = () => {
    if (cursorTimer) {
      clearInterval(cursorTimer)
      cursorTimer = null
    }
    cursorVisible.value = true
  }

  // 开始打字
  const start = () => {
    if (isTyping.value) return

    isTyping.value = true
    isComplete.value = false
    currentIndex.value = 0
    displayText.value = ''
    
    startCursorBlink()

    const typeNextChar = () => {
      if (currentIndex.value < currentText.value.length) {
        displayText.value += currentText.value[currentIndex.value]
        currentIndex.value++
        
        timer = setTimeout(typeNextChar, speed)
      } else {
        // 打字完成
        isTyping.value = false
        isComplete.value = true
        
        if (!loop) {
          stopCursorBlink()
        } else {
          // 循环模式：延迟后重新开始
          setTimeout(() => {
            if (loop) {
              start()
            }
          }, 2000)
        }
      }
    }

    typeNextChar()
  }

  // 停止打字
  const stop = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    isTyping.value = false
    stopCursorBlink()
  }

  // 重置
  const reset = () => {
    stop()
    displayText.value = ''
    currentIndex.value = 0
    isComplete.value = false
  }

  // 立即完成
  const complete = () => {
    stop()
    displayText.value = currentText.value
    currentIndex.value = currentText.value.length
    isComplete.value = true
  }

  // 带光标的完整文本
  const textWithCursor = computed(() => {
    if (!showCursor) return displayText.value
    
    return displayText.value + (
      (isTyping.value || !isComplete.value) && cursorVisible.value 
        ? cursorChar 
        : ''
    )
  })

  // 监听文本变化
  watch(currentText, (newText, oldText) => {
    if (newText !== oldText) {
      if (autoStart) {
        reset()
        start()
      }
    }
  })

  // 自动开始
  if (autoStart && currentText.value) {
    start()
  }

  // 清理
  onUnmounted(() => {
    stop()
  })

  return {
    displayText: readonly(displayText),
    textWithCursor: readonly(textWithCursor),
    isTyping: readonly(isTyping),
    isComplete: readonly(isComplete),
    cursorVisible: readonly(cursorVisible),
    start,
    stop,
    reset,
    complete
  }
}

// 简化版本：直接返回带光标的文本
export function useSimpleTypewriter(
  text: string | Ref<string>,
  speed = 50
) {
  const { textWithCursor, isComplete, start, stop } = useTypewriter(text, { 
    speed,
    autoStart: true,
    showCursor: true
  })

  return {
    text: textWithCursor,
    isComplete,
    start,
    stop
  }
} 