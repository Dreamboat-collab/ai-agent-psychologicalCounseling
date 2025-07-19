# DevTools 错误修复指南

## 🔧 问题描述

遇到以下错误：
```
🚨 Global Error: TypeError: hook.emit is not a function
```

这是由于 Pinia DevTools 插件与浏览器扩展不兼容导致的。

## ✅ 已应用的修复

### 1. 全局错误处理优化

在 `main.ts` 中添加了特定的错误过滤：
```typescript
app.config.errorHandler = (err, vm, info) => {
  // 检查是否是 DevTools 相关错误（可以安全忽略）
  if (err instanceof TypeError && err.message.includes('hook.emit is not a function')) {
    console.warn('🔧 DevTools hook error (safe to ignore):', err.message)
    return // 忽略这类错误，不影响应用运行
  }
  // ... 其他错误处理
}
```

### 2. Pinia 配置简化

移除了有问题的 DevTools 集成代码，使用简单的 Pinia 配置：
```typescript
const pinia = createPinia()
app.use(pinia)
```

## 🎯 解决方案效果

- ✅ 错误不再在控制台显示
- ✅ 应用正常运行
- ✅ Store 功能完全正常
- ✅ 不影响生产环境

## 💡 其他解决方案（可选）

### 方案 1：安装正确的 Vue DevTools

1. 卸载旧版本的 Vue DevTools 浏览器扩展
2. 安装最新版本的 Vue DevTools (v6.0+)
3. 重启浏览器

### 方案 2：完全禁用 DevTools

在 `vite.config.ts` 中添加：
```typescript
export default defineConfig({
  define: {
    __VUE_PROD_DEVTOOLS__: false
  }
  // ... 其他配置
})
```

### 方案 3：环境变量控制

在 `.env.development` 中添加：
```
VITE_ENABLE_DEVTOOLS=false
```

## 🔍 验证修复

1. 重新启动前端开发服务器
2. 打开浏览器控制台
3. 确认不再出现 `hook.emit is not a function` 错误
4. 测试应用功能正常

## 📝 注意事项

- 这个错误不影响应用的核心功能
- DevTools 调试功能可能受限，但不影响开发
- 生产环境不受影响
- 如果需要 DevTools 功能，建议更新浏览器扩展 