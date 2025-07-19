<template>
  <span class="typewriter-text" :class="{ 'is-typing': isTyping }">
    <span v-html="formattedText"></span>
    <span
      v-if="showCursor && cursorVisible"
      class="cursor animate-blink"
      :style="{ color: cursorColor }"
    >
      {{ cursorChar }}
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { marked } from 'marked'
import { useTypewriter } from '@/composables/useTypewriter'

interface Props {
  text: string
  speed?: number
  autoStart?: boolean
  showCursor?: boolean
  cursorChar?: string
  cursorColor?: string
  enableMarkdown?: boolean
  onComplete?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  speed: 50,
  autoStart: true,
  showCursor: true,
  cursorChar: '|',
  cursorColor: '#3b82f6',
  enableMarkdown: true
})

const {
  displayText,
  isTyping,
  isComplete,
  cursorVisible,
  start,
  stop,
  reset,
  complete
} = useTypewriter(props.text, {
  speed: props.speed,
  autoStart: props.autoStart,
  showCursor: false // 我们自己控制光标显示
})

// 格式化文本（支持Markdown）
const formattedText = computed(() => {
  if (!props.enableMarkdown) {
    return displayText.value.replace(/\n/g, '<br>')
  }
  
  try {
    return marked(displayText.value || '', {
      breaks: true,
      gfm: true
    })
  } catch (error) {
    console.warn('Markdown parse error:', error)
    return displayText.value.replace(/\n/g, '<br>')
  }
})

// 监听完成状态
watch(isComplete, (completed) => {
  if (completed && props.onComplete) {
    props.onComplete()
  }
})

// 暴露方法给父组件
defineExpose({
  start,
  stop,
  reset,
  complete,
  isTyping,
  isComplete
})
</script>

<style scoped>
.typewriter-text {
  @apply inline-block;
  word-wrap: break-word;
  word-break: break-word;
}

.typewriter-text.is-typing {
  @apply relative;
}

.cursor {
  @apply inline-block font-mono font-medium;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Markdown样式 */
.typewriter-text :deep(h1),
.typewriter-text :deep(h2),
.typewriter-text :deep(h3) {
  @apply font-bold mb-2 mt-4;
}

.typewriter-text :deep(h1) {
  @apply text-xl;
}

.typewriter-text :deep(h2) {
  @apply text-lg;
}

.typewriter-text :deep(h3) {
  @apply text-base;
}

.typewriter-text :deep(p) {
  @apply mb-2;
}

.typewriter-text :deep(ul),
.typewriter-text :deep(ol) {
  @apply ml-4 mb-2;
}

.typewriter-text :deep(li) {
  @apply mb-1;
}

.typewriter-text :deep(code) {
  @apply bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded text-sm font-mono;
}

.typewriter-text :deep(pre) {
  @apply bg-gray-100 dark:bg-gray-800 p-3 rounded-lg overflow-x-auto mb-2;
}

.typewriter-text :deep(pre code) {
  @apply bg-transparent p-0;
}

.typewriter-text :deep(blockquote) {
  @apply border-l-4 border-blue-500 pl-4 py-2 bg-blue-50 dark:bg-blue-900/20 mb-2;
}

.typewriter-text :deep(a) {
  @apply text-blue-600 dark:text-blue-400 underline hover:text-blue-800 dark:hover:text-blue-200;
}

.typewriter-text :deep(strong) {
  @apply font-bold;
}

.typewriter-text :deep(em) {
  @apply italic;
}
</style> 