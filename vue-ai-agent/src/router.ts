import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由定义
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/ChatView.vue'),
    meta: {
      title: 'AI Agent Chat - 智能对话助手',
      description: '与AI智能助手进行对话，体验先进的人工智能技术'
    }
  },
  {
    path: '/chat/:conversationId?',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: {
      title: '聊天对话 - AI Agent',
      description: '继续您的AI对话'
    }
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/TestView.vue'),
    meta: {
      title: 'AI Agent 测试页面',
      description: '测试 AI Agent 的基本功能'
    }
  },
  // 404 页面 - 重定向到首页
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    redirect: '/'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的滚动位置，恢复到该位置
    if (savedPosition) {
      return savedPosition
    }
    
    // 如果是锚点链接，滚动到对应元素
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }
    
    // 否则滚动到顶部
    return { top: 0 }
  }
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = to.meta.title as string
  }
  
  // 设置页面描述
  if (to.meta?.description) {
    const metaDescription = document.querySelector('meta[name="description"]')
    if (metaDescription) {
      metaDescription.setAttribute('content', to.meta.description as string)
    }
  }
  
  // 路由动画准备
  const app = document.getElementById('app')
  if (app) {
    app.classList.add('page-transitioning')
  }
  
  next()
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 页面加载完成后的处理
  setTimeout(() => {
    const app = document.getElementById('app')
    if (app) {
      app.classList.remove('page-transitioning')
    }
  }, 300)
  
  // 发送页面浏览统计（如果需要）
  console.log(`📱 Navigation: ${from.path} → ${to.path}`)
})

// 路由错误处理
router.onError((error) => {
  console.error('🚨 Router Error:', error)
  
  // 可以在这里添加错误上报逻辑
  // 比如发送到错误监控服务
})

export default router

// 导出路由工具函数
export const routeHelpers = {
  /**
   * 导航到聊天页面
   */
  goToChat: (conversationId?: string) => {
    if (conversationId) {
      return router.push({ name: 'Chat', params: { conversationId } })
    } else {
      return router.push({ name: 'Home' })
    }
  },

  /**
   * 返回上一页
   */
  goBack: () => {
    return router.back()
  },

  /**
   * 获取当前路由信息
   */
  getCurrentRoute: () => {
    return router.currentRoute.value
  },

  /**
   * 检查是否是特定路由
   */
  isRoute: (name: string) => {
    return router.currentRoute.value.name === name
  }
} 