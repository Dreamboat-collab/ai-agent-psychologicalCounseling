<template>
  <el-dialog
    v-model="visible"
    title="设置"
    width="600px"
    :before-close="handleClose"
    class="settings-dialog"
  >
    <div class="settings-content">
      <!-- 设置标签页 -->
      <el-tabs v-model="activeTab" type="border-card">
        
        <!-- 外观设置 -->
        <el-tab-pane label="外观" name="appearance">
          <div class="settings-section">
            <h3 class="section-title">主题设置</h3>
            
            <!-- 主题模式 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>主题模式</span>
                <small>选择应用的显示主题</small>
              </div>
              <el-radio-group v-model="localConfig.theme.mode" @change="handleThemeChange">
                <el-radio label="light">浅色</el-radio>
                <el-radio label="dark">深色</el-radio>
              </el-radio-group>
            </div>
            
            <!-- 主题色彩 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>主题色彩</span>
                <small>自定义应用的主色调</small>
              </div>
              <div class="color-presets">
                <div
                  v-for="preset in themePresets"
                  :key="preset.color"
                  class="color-preset"
                  :class="{ active: localConfig.theme.primaryColor === preset.color }"
                  :style="{ backgroundColor: preset.color }"
                  @click="handleColorChange(preset.color)"
                  :title="preset.name"
                />
              </div>
            </div>
            
            <!-- 字体大小 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>字体大小</span>
                <small>调整应用文字的大小</small>
              </div>
              <el-radio-group v-model="localConfig.app.fontSize" @change="handleFontSizeChange">
                <el-radio label="small">小</el-radio>
                <el-radio label="medium">中</el-radio>
                <el-radio label="large">大</el-radio>
              </el-radio-group>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 聊天设置 -->
        <el-tab-pane label="聊天" name="chat">
          <div class="settings-section">
            <h3 class="section-title">对话设置</h3>
            
            <!-- 打字机效果 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>打字机效果</span>
                <small>逐字符显示AI回复</small>
              </div>
              <el-switch
                v-model="localConfig.agent.typewriter.enabled"
                @change="handleTypewriterChange"
              />
            </div>
            
            <!-- 打字机速度 -->
            <div v-if="localConfig.agent.typewriter.enabled" class="setting-item">
              <div class="setting-label">
                <span>打字机速度</span>
                <small>调整文字显示的速度</small>
              </div>
              <div class="slider-container">
                <el-slider
                  v-model="localConfig.agent.typewriter.delay"
                  :min="10"
                  :max="200"
                  :step="10"
                  :format-tooltip="formatSpeedTooltip"
                  @change="handleTypewriterChange"
                />
                <span class="slider-value">{{ localConfig.agent.typewriter.delay }}ms</span>
              </div>
            </div>
            
            <!-- 自动滚动 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>自动滚动</span>
                <small>新消息时自动滚动到底部</small>
              </div>
              <el-switch v-model="localConfig.app.autoScroll" />
            </div>
            
            <!-- 显示时间戳 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>显示时间戳</span>
                <small>在消息旁显示发送时间</small>
              </div>
              <el-switch v-model="localConfig.app.showTimestamp" />
            </div>
            
            <!-- 紧凑模式 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>紧凑模式</span>
                <small>减小界面元素间距</small>
              </div>
              <el-switch v-model="localConfig.app.compactMode" />
            </div>
            
            <!-- 最大消息数 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>最大消息数</span>
                <small>每个对话保留的最大消息数量</small>
              </div>
              <el-input-number
                v-model="localConfig.agent.maxMessages"
                :min="10"
                :max="1000"
                :step="10"
                size="small"
                style="width: 120px"
              />
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 高级设置 -->
        <el-tab-pane label="高级" name="advanced">
          <div class="settings-section">
            <h3 class="section-title">数据管理</h3>
            
            <!-- 自动保存 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>自动保存</span>
                <small>自动保存对话记录到本地</small>
              </div>
              <el-switch v-model="localConfig.agent.autoSave" />
            </div>
            
            <!-- 声音提示 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>声音提示</span>
                <small>收到新消息时播放提示音</small>
              </div>
              <el-switch v-model="localConfig.agent.soundEnabled" />
            </div>
            
            <!-- 导出/导入配置 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>配置管理</span>
                <small>导出或导入设置配置</small>
              </div>
              <div class="setting-actions">
                <el-button size="small" @click="handleExportConfig">
                  导出配置
                </el-button>
                <el-button size="small" @click="handleImportConfig">
                  导入配置
                </el-button>
              </div>
            </div>
            
            <!-- 重置设置 -->
            <div class="setting-item">
              <div class="setting-label">
                <span>重置设置</span>
                <small>恢复所有设置到默认值</small>
              </div>
              <el-button size="small" type="danger" @click="handleResetSettings">
                重置设置
              </el-button>
            </div>
          </div>
        </el-tab-pane>
        
        <!-- 关于 -->
        <el-tab-pane label="关于" name="about">
          <div class="settings-section">
            <div class="about-content">
              <div class="app-logo">
                <div class="logo-icon">🤖</div>
                <h2 class="app-name">AI Agent Chat</h2>
                <p class="app-version">版本 1.0.0</p>
              </div>
              
              <div class="app-description">
                <p>基于Vue 3 + TypeScript的现代化AI智能对话助手，支持实时流式对话、打字机效果、RAG知识库和完整的会话管理功能。</p>
              </div>
              
              <div class="app-features">
                <h4>主要特性</h4>
                <ul>
                  <li>🤖 智能对话与RAG知识库</li>
                  <li>⚡ 实时流式响应</li>
                  <li>📝 打字机效果</li>
                  <li>💾 完整会话管理</li>
                  <li>🎨 自定义主题</li>
                  <li>📱 响应式设计</li>
                </ul>
              </div>
              
              <div class="app-info">
                <div class="info-item">
                  <strong>技术栈：</strong>
                  <span>Vue 3, TypeScript, Element Plus, Tailwind CSS</span>
                </div>
                <div class="info-item">
                  <strong>许可证：</strong>
                  <span>MIT License</span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存设置</el-button>
      </div>
    </template>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".json"
      style="display: none"
      @change="handleFileImport"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConfigStore } from '@/stores/config'
import type { ThemeConfig, AgentConfig } from '@/api/types'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'update-config', config: any): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Store
const configStore = useConfigStore()

// 响应式数据
const activeTab = ref('appearance')
const fileInputRef = ref<HTMLInputElement>()

// 本地配置副本
const localConfig = ref({
  theme: { ...configStore.theme },
  agent: { ...configStore.agent },
  app: { ...configStore.app }
})

// 计算属性
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const themePresets = computed(() => configStore.themePresets)

// 监听器
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    // 对话框打开时重新加载配置
    localConfig.value = {
      theme: { ...configStore.theme },
      agent: { ...configStore.agent },
      app: { ...configStore.app }
    }
  }
})

// 方法
const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  try {
    // 应用主题变化
    configStore.theme = { ...localConfig.value.theme }
    configStore.agent = { ...localConfig.value.agent }
    configStore.app = { ...localConfig.value.app }
    
    // 应用主题
    configStore.applyTheme()
    
    // 更新打字机配置
    if (localConfig.value.agent.typewriter.enabled !== configStore.agent.typewriter.enabled ||
        localConfig.value.agent.typewriter.delay !== configStore.agent.typewriter.delay) {
      await configStore.updateTypewriterConfig(localConfig.value.agent.typewriter)
    }
    
    emit('update-config', localConfig.value)
    ElMessage.success('设置已保存')
    handleClose()
    
  } catch (error) {
    console.error('Save settings error:', error)
    ElMessage.error('保存设置失败')
  }
}

const handleThemeChange = () => {
  // 实时预览主题变化
  const root = document.documentElement
  if (localConfig.value.theme.mode === 'dark') {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
}

const handleColorChange = (color: string) => {
  localConfig.value.theme.primaryColor = color
  
  // 实时预览颜色变化
  const root = document.documentElement
  root.style.setProperty('--primary-color', color)
}

const handleFontSizeChange = () => {
  // 实时预览字体大小变化
  const multiplier = localConfig.value.app.fontSize === 'small' ? '0.875' :
                    localConfig.value.app.fontSize === 'large' ? '1.125' : '1'
  
  const root = document.documentElement
  root.style.setProperty('--font-size-multiplier', multiplier)
}

const handleTypewriterChange = async () => {
  // 可以在这里添加实时预览逻辑
}

const formatSpeedTooltip = (value: number) => {
  const speed = value <= 50 ? '快' : value <= 100 ? '中' : '慢'
  return `${speed} (${value}ms)`
}

const handleExportConfig = () => {
  configStore.exportConfig()
  ElMessage.success('配置已导出')
}

const handleImportConfig = () => {
  fileInputRef.value?.click()
}

const handleFileImport = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const configData = JSON.parse(e.target?.result as string)
      const success = configStore.importConfig(configData)
      
      if (success) {
        // 重新加载本地配置
        localConfig.value = {
          theme: { ...configStore.theme },
          agent: { ...configStore.agent },
          app: { ...configStore.app }
        }
        ElMessage.success('配置导入成功')
      } else {
        ElMessage.error('配置文件格式错误')
      }
    } catch (error) {
      console.error('Import config error:', error)
      ElMessage.error('配置文件解析失败')
    }
  }
  
  reader.readAsText(file)
  
  // 清空文件输入
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

const handleResetSettings = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置所有设置到默认值吗？此操作不可撤销。',
      '确认重置',
      {
        confirmButtonText: '重置',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    configStore.resetToDefaults()
    
    // 重新加载本地配置
    localConfig.value = {
      theme: { ...configStore.theme },
      agent: { ...configStore.agent },
      app: { ...configStore.app }
    }
    
    ElMessage.success('设置已重置')
    
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.settings-dialog {
  @apply rounded-lg;
}

.settings-content {
  @apply max-h-96 overflow-y-auto;
}

.settings-section {
  @apply space-y-6;
}

.section-title {
  @apply text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4;
  @apply border-b border-gray-200 dark:border-gray-700 pb-2;
}

.setting-item {
  @apply flex items-center justify-between py-3;
  @apply border-b border-gray-100 dark:border-gray-800 last:border-b-0;
}

.setting-label {
  @apply flex-1 min-w-0 mr-4;
}

.setting-label span {
  @apply block font-medium text-gray-900 dark:text-gray-100;
}

.setting-label small {
  @apply block text-sm text-gray-500 dark:text-gray-400 mt-1;
}

.setting-actions {
  @apply flex gap-2;
}

/* 颜色预设 */
.color-presets {
  @apply flex gap-2;
}

.color-preset {
  @apply w-8 h-8 rounded-full cursor-pointer;
  @apply border-2 border-transparent transition-all;
  @apply hover:scale-110;
}

.color-preset.active {
  @apply border-gray-400 dark:border-gray-300;
  @apply shadow-lg scale-110;
}

/* 滑块容器 */
.slider-container {
  @apply flex items-center gap-3 min-w-48;
}

.slider-value {
  @apply text-sm text-gray-600 dark:text-gray-400 font-mono;
  @apply min-w-12 text-right;
}

/* 关于页面 */
.about-content {
  @apply text-center space-y-6;
}

.app-logo {
  @apply space-y-2;
}

.logo-icon {
  @apply text-6xl;
}

.app-name {
  @apply text-2xl font-bold text-gray-900 dark:text-gray-100;
}

.app-version {
  @apply text-gray-500 dark:text-gray-400;
}

.app-description {
  @apply text-gray-600 dark:text-gray-400 leading-relaxed;
}

.app-features {
  @apply text-left;
}

.app-features h4 {
  @apply font-semibold text-gray-900 dark:text-gray-100 mb-2;
}

.app-features ul {
  @apply space-y-1;
}

.app-features li {
  @apply text-gray-600 dark:text-gray-400;
}

.app-info {
  @apply space-y-2 text-left;
}

.info-item {
  @apply flex flex-wrap gap-2;
}

.info-item strong {
  @apply text-gray-900 dark:text-gray-100;
}

.info-item span {
  @apply text-gray-600 dark:text-gray-400;
}

/* 底部按钮 */
.dialog-footer {
  @apply flex justify-end gap-2;
}

/* 标签页样式覆盖 */
:deep(.el-tabs__content) {
  @apply p-4;
}

:deep(.el-tab-pane) {
  @apply outline-none;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .setting-item {
    @apply flex-col items-start gap-3;
  }
  
  .setting-label {
    @apply mr-0;
  }
  
  .slider-container {
    @apply w-full min-w-0;
  }
  
  .color-presets {
    @apply flex-wrap;
  }
}

/* 暗色主题适配 */
.dark .settings-dialog {
  @apply bg-gray-800;
}

.dark .setting-item {
  @apply border-gray-700;
}

/* 动画效果 */
.color-preset {
  transition: all 0.2s ease;
}

.setting-item {
  transition: background-color 0.2s ease;
}

.setting-item:hover {
  @apply bg-gray-50 dark:bg-gray-700/50;
}
</style> 