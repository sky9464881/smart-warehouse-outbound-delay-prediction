import { createRouter, createWebHistory } from 'vue-router'
import pinia from '@/stores/pinia'
import { useAuthStore } from '@/stores/auth'
import DashboardLayout from '../views/DashboardLayout.vue'
import SummaryView from '../views/dashboard/SummaryView.vue'
import DelayFlowView from '../views/dashboard/DelayFlowView.vue'
import DemandPressureView from '../views/dashboard/DemandPressureView.vue'
import CapacityView from '../views/dashboard/CapacityView.vue'
import FlowFrictionView from '../views/dashboard/FlowFrictionView.vue'
import LoginView from '../views/auth/LoginView.vue'
import SignupView from '../views/auth/SignupView.vue'
import PermissionsView from '../views/admin/PermissionsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { requiresGuest: true },
    },
    {
      path: '/signup',
      name: 'signup',
      component: SignupView,
      meta: { requiresGuest: true },
    },
    {
      path: '/',
      component: DashboardLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'summary',
          component: SummaryView,
        },
        {
          path: 'delay-flow',
          name: 'delayFlow',
          component: DelayFlowView,
        },
        {
          path: 'demand-pressure',
          name: 'demandPressure',
          component: DemandPressureView,
        },
        {
          path: 'capacity',
          name: 'capacity',
          component: CapacityView,
        },
        {
          path: 'flow-friction',
          name: 'flowFriction',
          component: FlowFrictionView,
        },
        {
          path: 'admin/permissions',
          name: 'adminPermissions',
          component: PermissionsView,
          meta: { requiresAdmin: true },
        },
      ],
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)
  await auth.init()

  if (to.meta?.requiresGuest && auth.isAuthenticated) {
    return { name: 'summary' }
  }

  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta?.requiresAdmin && !auth.isGlobalAdmin) {
    return { name: 'summary' }
  }
})

export default router
