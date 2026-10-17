import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { title: '智能客服' },
    },
    {
      path: '/admin',
      component: () => import('@/views/AdminView.vue'),
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'tenants',
          name: 'TenantManagement',
          component: () => import('@/views/TenantManagement.vue'),
          meta: { title: '租户管理' },
        },
        {
          path: 'knowledge',
          name: 'KnowledgeManagement',
          component: () => import('@/views/KnowledgeManagement.vue'),
          meta: { title: '知识库管理' },
        },
      ],
    },
  ],
})

export default router
