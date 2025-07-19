# Vue AI Agent - 智能对话助手前端

![Vue AI Agent](https://img.shields.io/badge/Vue-3.4.0-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.3.3-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-5.0.8-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.4.4-409EFF?style=for-the-badge&logo=element&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/Tailwind%20CSS-3.3.6-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

> 基于Vue 3 + TypeScript的现代化AI智能对话助手前端应用，支持实时流式对话、打字机效果、RAG知识库和完整的会话管理功能。

## ✨ 功能特性

### 🎯 核心功能
- **🤖 智能对话**: 与AI助手进行自然语言对话
- **⚡ 流式响应**: 实时接收AI回复，无需等待
- **📝 打字机效果**: 逐字符显示，模拟真实打字体验
- **🧠 RAG知识库**: 集成检索增强生成，提供准确信息
- **💾 会话管理**: 创建、切换、删除、重命名对话
- **🔄 消息重试**: 重新生成AI回复
- **📱 响应式设计**: 完美适配桌面端和移动端

### 🎨 用户体验
- **🌓 深色/浅色主题**: 自动跟随系统或手动切换
- **⌨️ 快捷键支持**: 提高操作效率
- **🎛️ 个性化设置**: 自定义字体大小、主题色彩等
- **📤 数据导出**: 导出对话记录和配置
- **🎯 无障碍支持**: 符合WCAG标准

### 🛠️ 技术特性
- **📱 PWA支持**: 离线使用和应用安装
- **🔄 自动保存**: 本地存储对话和设置
- **⚡ 性能优化**: 虚拟滚动、懒加载等
- **🐛 错误边界**: 优雅的错误处理
- **📊 性能监控**: 加载时间和内存使用统计

## 🚀 快速开始

### 📋 环境要求

在开始之前，请确保你的开发环境满足以下要求：

- **Node.js**: >= 18.0.0
- **npm**: >= 9.0.0 或 **yarn**: >= 1.22.0 或 **pnpm**: >= 8.0.0
- **Git**: 最新版本

### 📦 安装依赖

1. **克隆项目**
```bash
git clone <your-repository-url>
cd vue-ai-agent
```

2. **安装依赖**

使用 npm:
```bash
npm install
```

使用 yarn:
```bash
yarn install
```

使用 pnpm (推荐):
```bash
pnpm install
```

### ⚙️ 环境配置

1. **后端服务配置**

在启动前端之前，请确保后端AI Agent服务已经运行：

```bash
# 后端服务默认运行在 http://localhost:8080
# 前端会自动代理API请求到后端服务
```

2. **环境变量 (可选)**

创建 `.env.local` 文件来自定义配置：

```bash
# API基础URL (可选，默认使用代理)
VITE_API_BASE_URL=http://localhost:8080

# 应用标题
VITE_APP_TITLE=AI Agent Chat

# 是否启用PWA
VITE_ENABLE_PWA=true

# 是否启用错误上报
VITE_ENABLE_ERROR_REPORTING=false
```

### 🎬 启动开发服务器

```bash
# 使用 npm
npm run dev

# 使用 yarn
yarn dev

# 使用 pnpm
pnpm dev
```

启动成功后，访问 http://localhost:3000 即可看到应用界面。

### 🔨 构建生产版本

```bash
# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

构建完成后，`dist` 目录包含所有生产文件。

## 📁 项目结构

```
vue-ai-agent/
├── public/                     # 静态资源
│   ├── favicon.ico
│   └── index.html
├── src/
│   ├── api/                    # API接口层
│   │   ├── agent.ts           # Agent相关接口
│   │   └── types.ts           # 类型定义
│   ├── components/            # Vue组件
│   │   ├── chat/             # 聊天相关组件
│   │   │   ├── ChatWindow.vue
│   │   │   ├── MessageItem.vue
│   │   │   ├── TypewriterText.vue
│   │   │   └── InputBox.vue
│   │   └── common/           # 通用组件
│   ├── composables/          # 组合式函数
│   │   ├── useChat.ts
│   │   ├── useSSE.ts
│   │   └── useTypewriter.ts
│   ├── stores/               # Pinia状态管理
│   │   ├── chat.ts
│   │   └── config.ts
│   ├── styles/               # 样式文件
│   │   └── global.css
│   ├── views/                # 页面视图
│   │   └── ChatView.vue
│   ├── App.vue               # 根组件
│   ├── main.ts               # 入口文件
│   └── router.ts             # 路由配置
├── package.json
├── vite.config.ts             # Vite配置
├── tailwind.config.js         # Tailwind配置
├── tsconfig.json              # TypeScript配置
└── README.md                  # 项目文档
```

## 🎮 使用指南

### 💬 基本对话

1. **启动对话**: 在输入框中输入你的问题
2. **发送消息**: 按 `Enter` 键或点击发送按钮
3. **查看回复**: AI会以打字机效果实时显示回复
4. **管理对话**: 使用左侧面板管理多个对话

### ⌨️ 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + N` | 新建对话 |
| `Ctrl + /` | 切换侧边栏 |
| `Ctrl + ,` | 打开设置 |
| `Ctrl + D` | 切换主题 |
| `Enter` | 发送消息 |
| `Shift + Enter` | 换行 |
| `Ctrl + Enter` | 强制发送 |
| `Escape` | 清空输入 |
| `?` 或 `F1` | 显示帮助 |

### 🛠️ 自定义配置

访问设置页面可以自定义：

- **主题设置**: 深色/浅色模式、主题色彩
- **字体设置**: 字体大小调整
- **打字机效果**: 启用/禁用、速度调整
- **聊天设置**: 自动滚动、时间戳显示等

### 📤 数据管理

- **导出对话**: 将对话记录导出为JSON文件
- **导出配置**: 备份个人设置
- **导入配置**: 恢复之前的设置

## 🔧 开发指南

### 🏗️ 添加新功能

1. **创建组件**:
```vue
<template>
  <div class="my-component">
    <!-- 组件内容 -->
  </div>
</template>

<script setup lang="ts">
// TypeScript代码
</script>

<style scoped>
/* 样式代码 */
</style>
```

2. **添加状态管理**:
```typescript
// stores/myStore.ts
import { defineStore } from 'pinia'

export const useMyStore = defineStore('myStore', () => {
  // 状态和方法
})
```

3. **添加API接口**:
```typescript
// api/myAPI.ts
export const myAPI = {
  async getData() {
    // API调用逻辑
  }
}
```

### 🧪 代码规范

项目使用以下工具确保代码质量：

- **ESLint**: 代码规范检查
- **Prettier**: 代码格式化
- **TypeScript**: 类型检查
- **Husky**: Git hooks

运行检查命令：
```bash
# 代码规范检查
npm run lint

# 代码格式化
npm run format

# 类型检查
npm run type-check
```

### 📱 响应式设计

项目使用 Tailwind CSS 实现响应式设计：

```css
/* 移动端优先 */
.my-class {
  @apply text-sm;
}

/* 平板端 */
@media (min-width: 768px) {
  .my-class {
    @apply text-base;
  }
}

/* 桌面端 */
@media (min-width: 1024px) {
  .my-class {
    @apply text-lg;
  }
}
```

## 🐛 常见问题

### ❓ 启动失败

**问题**: 运行 `npm run dev` 时报错

**解决方案**:
1. 检查Node.js版本是否 >= 18.0.0
2. 删除 `node_modules` 文件夹重新安装依赖
3. 检查端口3000是否被占用

```bash
# 检查Node.js版本
node --version

# 重新安装依赖
rm -rf node_modules package-lock.json
npm install

# 检查端口占用
netstat -an | grep 3000
```

### ❓ 连接后端失败

**问题**: 前端无法连接到后端服务

**解决方案**:
1. 确保后端服务在 http://localhost:8080 运行
2. 检查防火墙设置
3. 查看浏览器开发者工具的网络标签页

### ❓ 构建失败

**问题**: 运行 `npm run build` 时报错

**解决方案**:
1. 检查TypeScript类型错误
2. 确保所有依赖正确安装
3. 检查环境变量配置

```bash
# 检查类型错误
npm run type-check

# 清理缓存
npm run dev -- --force
```

### ❓ 样式问题

**问题**: Tailwind CSS样式不生效

**解决方案**:
1. 检查 `tailwind.config.js` 配置
2. 确保样式文件正确导入
3. 清理浏览器缓存

## 🤝 贡献指南

我们欢迎各种形式的贡献！

### 🔄 提交流程

1. **Fork 项目**
2. **创建特性分支**: `git checkout -b feature/amazing-feature`
3. **提交更改**: `git commit -m 'Add some amazing feature'`
4. **推送分支**: `git push origin feature/amazing-feature`
5. **创建 Pull Request**

### 📝 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 添加新功能
fix: 修复错误
docs: 更新文档
style: 代码格式化
refactor: 代码重构
test: 添加测试
chore: 构建过程或辅助工具的变动
```

### 🐛 Bug 报告

请在 GitHub Issues 中提交 Bug 报告，包含：

- 详细的问题描述
- 复现步骤
- 期望行为
- 实际行为
- 环境信息（OS、浏览器版本等）

## 📄 License

MIT License © 2024

---

## 🎉 开始使用

现在你已经了解了如何设置和使用 Vue AI Agent！

1. **启动后端服务** (在你的Java Spring Boot项目中)
2. **运行前端开发服务器**: `npm run dev`
3. **访问 http://localhost:3000**
4. **开始与AI对话！**

如果遇到任何问题，请查看上面的常见问题部分或在 GitHub Issues 中寻求帮助。

**祝你使用愉快！** 🚀✨ 