<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Leafer, Ellipse, Line, Text, Group } from 'leafer-ui'
import { t, locale } from '../i18n/index.js'
import { resolved } from '../theme.js'

const root = ref(null)
let leafer = null
let rafId = null
let titleNode = null
let tagsNode = null
const gridLines = []
const rings = []
const dots = []
const DOTS_N = 14

// canvas 不吃 CSS 变量，颜色只能按主题手写两份
const COLORS = {
  dark: {
    grid: 'rgba(255,255,255,0.05)',
    ringA: 'rgba(65, 209, 165, 0.4)',
    ringB: 'rgba(91, 141, 239, 0.5)',
    title: 'rgba(65, 209, 165, 0.7)',
    tags: 'rgba(157, 196, 255, 0.55)',
    dotA: '#41d1a5',
    dotB: '#5b8def',
  },
  light: {
    grid: 'rgba(16,24,40,0.07)',
    ringA: 'rgba(15, 155, 118, 0.45)',
    ringB: 'rgba(47, 111, 208, 0.45)',
    title: 'rgba(15, 118, 92, 0.85)',
    tags: 'rgba(71, 105, 170, 0.75)',
    dotA: '#0f9b76',
    dotB: '#2f6fd0',
  },
}

function applyColors() {
  const c = COLORS[resolved.value] || COLORS.dark
  gridLines.forEach((n) => (n.stroke = c.grid))
  if (rings[0]) rings[0].stroke = c.ringA
  if (rings[1]) rings[1].stroke = c.ringB
  if (titleNode) titleNode.fill = c.title
  if (tagsNode) tagsNode.fill = c.tags
  dots.forEach((d) => (d.node.fill = d.blue ? c.dotB : c.dotA))
}

function init() {
  const el = root.value
  if (!el) return
  const parent = el.parentElement
  const width = parent?.clientWidth || window.innerWidth
  const height = 220

  leafer = new Leafer({
    view: el,
    width,
    height,
    wheel: false,
    hittable: false,
  })

  const c = COLORS[resolved.value] || COLORS.dark

  // 背景横线（淡淡的）
  const gridGroup = new Group()
  for (let i = 0; i < 8; i++) {
    const line = new Line({
      points: [0, 28 + i * 24, width, 28 + i * 24],
      stroke: c.grid,
      strokeWidth: 1,
    })
    gridGroup.add(line)
    gridLines.push(line)
  }
  leafer.add(gridGroup)

  // 两个同心圆环
  const ringA = new Ellipse({
    x: width * 0.18,
    y: 110,
    width: 130,
    height: 130,
    stroke: c.ringA,
    strokeWidth: 1.2,
    fill: 'transparent',
  })
  const ringB = new Ellipse({
    x: width * 0.18 + 25,
    y: 110 + 25,
    width: 80,
    height: 80,
    stroke: c.ringB,
    strokeWidth: 1.2,
    fill: 'transparent',
  })
  leafer.add(ringA)
  leafer.add(ringB)
  rings.push(ringA, ringB)

  // 标签（语言切换时只更新文本，不重建画布）
  titleNode = new Text({
    x: 22,
    y: 14,
    fill: c.title,
    text: t('decor.title'),
    fontSize: 12,
  })
  leafer.add(titleNode)

  // 流动的小圆点（消息粒子）
  for (let i = 0; i < DOTS_N; i++) {
    const isBlue = i % 3 === 0
    const dot = new Ellipse({
      x: width * 0.42 + i * (width * 0.55 / DOTS_N),
      y: 96 + Math.sin(i * 0.7) * 26,
      width: 8,
      height: 8,
      fill: isBlue ? c.dotB : c.dotA,
      opacity: 0.85,
    })
    leafer.add(dot)
    dots.push({ node: dot, phase: Math.random() * Math.PI * 2, blue: isBlue })
  }

  // 右侧标签（语言切换时只更新文本，不重建画布）
  tagsNode = new Text({
    x: width - 320,
    y: 14,
    fill: c.tags,
    text: t('decor.tags'),
    fontSize: 12,
  })
  leafer.add(tagsNode)
}

function tick(now) {
  rafId = requestAnimationFrame(tick)
  if (!leafer || !dots.length) return
  const time = now * 0.001
  dots.forEach((d, i) => {
    const baseX = parseFloat(d.node.x)
    const baseY = 96
    d.node.x = baseX + Math.sin(time + d.phase) * 1.2
    d.node.y = baseY + Math.sin(time * 0.9 + d.phase) * 14
    d.node.opacity = 0.4 + 0.4 * (0.5 + 0.5 * Math.sin(time * 0.7 + d.phase))
  })
}

function onResize() {
  if (!leafer || !root.value) return
  const width = root.value.parentElement?.clientWidth || window.innerWidth
  leafer.width = width
  if (typeof leafer.forceRender === 'function') leafer.forceRender()
}

onMounted(() => {
  init()
  rafId = requestAnimationFrame(tick)
  window.addEventListener('resize', onResize)
})

// 切语言：canvas 里两段文字原地换掉，不重建画布
watch(locale, () => {
  if (titleNode) titleNode.text = t('decor.title')
  if (tagsNode) tagsNode.text = t('decor.tags')
})

watch(resolved, applyColors)

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  leafer?.destroy?.()
})
</script>

<template>
  <div ref="root" class="decor" aria-hidden="true"></div>
</template>

<style scoped>
.decor {
  position: relative;
  width: 100%;
  height: 220px;
  margin-top: -40px;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  background: var(--decor-bg);
  overflow: hidden;
}

.decor :deep(canvas) {
  display: block;
  width: 100%;
  height: 100%;
}
</style>