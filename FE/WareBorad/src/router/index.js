import { createRouter, createWebHistory } from 'vue-router'
import DashboardLayout from '../views/DashboardLayout.vue'
import SummaryView from '../views/dashboard/SummaryView.vue'
import DelayFlowView from '../views/dashboard/DelayFlowView.vue'
import DemandPressureView from '../views/dashboard/DemandPressureView.vue'
import CapacityView from '../views/dashboard/CapacityView.vue'
import FlowFrictionView from '../views/dashboard/FlowFrictionView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: DashboardLayout,
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
      ],
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
