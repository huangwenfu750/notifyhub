<script setup>
import SiteHero from '../components/SiteHero.vue'
import Architecture from '../components/Architecture.vue'
import FeatureGrid from '../components/FeatureGrid.vue'
import CodeTabs from '../components/CodeTabs.vue'
import QuickStart from '../components/QuickStart.vue'
import DeployCards from '../components/DeployCards.vue'
import InstallTable from '../components/InstallTable.vue'
import { useReveal } from '../composables/useReveal.js'

// 放在首页内部：切到文档页时 ctx.revert() 会连同 ScrollTrigger 一起清掉，
// 切回来再重新注册，避免 trigger 元素消失后残留监听
useReveal()
</script>

<template>
  <main>
    <SiteHero />
    <Architecture />
    <FeatureGrid />
    <CodeTabs />
    <QuickStart />
    <DeployCards />
    <InstallTable />
  </main>
</template>
