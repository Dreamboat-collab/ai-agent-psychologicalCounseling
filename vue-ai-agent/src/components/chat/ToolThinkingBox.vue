<template>
  <div class="tool-thinking-box">
    <el-collapse v-model="activeCollapse" accordion>
      <el-collapse-item 
        :name="message.id" 
        class="thinking-collapse"
      >
        <template #title>
          <div class="thinking-header">
            <el-icon class="thinking-icon">
              <Tools />
            </el-icon>
            <span class="thinking-title">AI思考过程</span>
            <el-tag 
              size="small" 
              type="info" 
              effect="plain"
              class="thinking-tag"
            >
              工具调用
            </el-tag>
          </div>
        </template>
        
        <div class="thinking-content">
          <div class="thinking-text" v-html="formattedContent"></div>
          
          <!-- 操作按钮 -->
          <div class="thinking-actions" v-if="showActions">
            <el-button
              link
              size="small"
              type="primary"
              @click="copyContent"
              :icon="CopyDocument"
            >
              复制
            </el-button>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import type { ChatMessage } from '@/api/types'

// 图标
import { 
  Tools,
  CopyDocument
} from '@element-plus/icons-vue'

interface Props {
  message: ChatMessage
  showActions?: boolean
  defaultExpanded?: boolean
}

interface Emits {
  (e: 'copy', content: string): void
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true,
  defaultExpanded: false
})

const emit = defineEmits<Emits>()

// 控制折叠状态
const activeCollapse = ref<string[]>(props.defaultExpanded ? [props.message.id] : [])

// 格式化内容
const formattedContent = computed(() => {
  if (!props.message.content) return ''
  
  try {
    return marked(props.message.content, {
      breaks: true,
      gfm: true
    })
  } catch (error) {
    console.warn('Markdown parse error:', error)
    return props.message.content.replace(/\n/g, '<br>')
  }
})

// 复制内容
const copyContent = async () => {
  try {
    await navigator.clipboard.writeText(props.message.content)
    ElMessage.success('思考过程已复制到剪贴板')
    emit('copy', props.message.content)
  } catch (error) {
    console.error('Copy failed:', error)
    ElMessage.error('复制失败')
  }
}
</script>

<style scoped>
.tool-thinking-box {
  @apply mb-3;
}

.thinking-collapse {
  @apply border border-orange-200 dark:border-orange-800 rounded-lg overflow-hidden;
  @apply bg-orange-50 dark:bg-orange-900/20;
}

.thinking-collapse :deep(.el-collapse-item__header) {
  @apply bg-orange-100 dark:bg-orange-900/30 border-none px-4 py-3;
  @apply hover:bg-orange-200 dark:hover:bg-orange-900/40;
  @apply transition-colors duration-200;
}

.thinking-collapse :deep(.el-collapse-item__content) {
  @apply px-4 pb-4 pt-0 border-none;
}

.thinking-header {
  @apply flex items-center gap-2 w-full;
}

.thinking-icon {
  @apply text-orange-600 dark:text-orange-400 text-lg;
}

.thinking-title {
  @apply font-medium text-orange-900 dark:text-orange-100 flex-1;
}

.thinking-tag {
  @apply ml-auto;
}

.thinking-content {
  @apply space-y-3;
}

.thinking-text {
  @apply text-sm text-gray-700 dark:text-gray-300 leading-relaxed;
  @apply bg-white dark:bg-gray-800 rounded p-3;
  @apply border border-gray-200 dark:border-gray-700;
  @apply font-mono;
}

.thinking-text :deep(pre) {
  @apply bg-gray-100 dark:bg-gray-900 p-2 rounded text-xs overflow-x-auto;
}

.thinking-text :deep(code) {
  @apply bg-gray-100 dark:bg-gray-900 px-1 py-0.5 rounded text-xs;
}

.thinking-actions {
  @apply flex justify-end gap-2 pt-2 border-t border-orange-200 dark:border-orange-700;
}

/* 动画效果 */
.thinking-collapse :deep(.el-collapse-item__wrap) {
  @apply transition-all duration-300;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .thinking-header {
    @apply text-sm;
  }
  
  .thinking-text {
    @apply text-xs;
  }
}
</style> 