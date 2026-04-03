import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台' }
      },
      // Phase 1: 前端市场洞察
      {
        path: 'market/heatmap',
        name: 'HeatMap',
        component: () => import('@/views/market/heatMap.vue'),
        meta: { title: '前端分布热力图' }
      },
      {
        path: 'market/insight',
        name: 'Insight',
        component: () => import('@/views/market/insight.vue'),
        meta: { title: '空白网点挖掘' }
      },
      {
        path: 'market/flow',
        name: 'Flow',
        component: () => import('@/views/market/flow.vue'),
        meta: { title: '订单流向' }
      },
      // Phase 2: 精准营销
      {
        path: 'user/select',
        name: 'SelectArea',
        component: () => import('@/views/user/selectArea.vue'),
        meta: { title: '产品推广选区' }
      },
      // Phase 3: 管理赋能
      {
        path: 'manage/weak',
        name: 'WeakArea',
        component: () => import('@/views/manage/weakArea.vue'),
        meta: { title: '薄弱区域洞察' }
      },
      {
        path: 'manage/evaluate',
        name: 'Evaluate',
        component: () => import('@/views/manage/evaluate.vue'),
        meta: { title: '人员效能评估' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
