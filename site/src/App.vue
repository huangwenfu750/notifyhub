<script setup>
import { computed, defineAsyncComponent, nextTick, watch } from 'vue'
import SiteNav from './components/SiteNav.vue'
import SiteFooter from './components/SiteFooter.vue'
import HomeView from './views/HomeView.vue'
import { route, isDocsPath } from './router.js'

// leafer-ui 是大块，单独拆 chunk 让浏览器独立缓存
const DecorLeafer = defineAsyncComponent(() => import('./components/DecorLeafer.vue'))
// 文档页（含两份语言的文档数据）只在真的点进 /docs 时才下载
const DocsView = defineAsyncComponent(() => import('./views/DocsView.vue'))

const view = computed(() => (isDocsPath(route.value) ? DocsView : HomeView))

// 从文档页点首页锚点（#features）时，要等 HomeView 渲染完再滚
watch(route, async () => {
  await nextTick()
  const id = window.location.hash.replace(/^#/, '')
  if (id && !id.startsWith('/')) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
})
</script>

<template>
  <SiteNav />
  <component :is="view" />
  <DecorLeafer />
  <SiteFooter />
</template>
