import { createRouter, createWebHistory } from 'vue-router'
import DiscoverView from '@/views/DiscoverView.vue'
import ChartsView from '@/views/ChartsView.vue'
import AIRecommendView from '@/views/AIRecommendView.vue'
import RadioView from '@/views/RadioView.vue'
import MyMusicView from '@/views/MyMusicView.vue'
import ImportView from '@/views/ImportView.vue'
import SearchView from '@/views/SearchView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/discover'
    },
    {
      path: '/discover',
      name: 'discover',
      component: DiscoverView
    },
    {
      path: '/charts',
      name: 'charts',
      component: ChartsView
    },
    {
      path: '/ai-recommend',
      name: 'ai-recommend',
      component: AIRecommendView
    },
    {
      path: '/radio',
      name: 'radio',
      component: RadioView
    },
    {
      path: '/my-music',
      name: 'my-music',
      component: MyMusicView
    },
    {
      path: '/import',
      name: 'import',
      component: ImportView
    },
    {
      path: '/search',
      name: 'search',
      component: SearchView
    }
  ]
})

export default router
