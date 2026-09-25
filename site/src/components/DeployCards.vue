<script setup>
import { nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import CodeWindow from './CodeWindow.vue'
import { t, content, locale } from '../i18n/index.js'

const stage = ref(null)
const track = ref(null)
let tween

function setup() {
  if (!stage.value || !track.value) return
  // 等一帧，让布局与字体先稳定，再算横向距离
  requestAnimationFrame(() => {
    const distance = track.value.scrollWidth - window.innerWidth
    if (distance <= 0) return // 视口足够宽，三卡已经全显示，不需要水平滚动
    const step = distance / 2 // 卡1→卡2、卡2→卡3 各走一半

    // 分段 timeline：每张卡先停留，再滑向下一张；滚动总距离拉长到位移的 2 倍，节奏更慢
    tween = gsap.timeline({
      defaults: { ease: 'none' },
      scrollTrigger: {
        trigger: stage.value,
        start: 'top top',
        end: () => `+=${distance * 2}`,
        scrub: 1.2,
        pin: true,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        // 停下来时吸附到三张卡各自的展示位
        snap: {
          snapTo: [0.09, 0.5, 0.91, 1],
          duration: { min: 0.3, max: 0.9 },
          delay: 0.08,
          ease: 'power1.inOut',
        },
      },
    })

    tween
      .to(track.value, { x: 0, duration: 0.9 }) // 卡 1 停留
      .to(track.value, { x: -step, duration: 1.1, ease: 'power2.inOut' }) // 滑到卡 2
      .to(track.value, { x: -step, duration: 0.9 }) // 卡 2 停留
      .to(track.value, { x: -distance, duration: 1.1, ease: 'power2.inOut' }) // 滑到卡 3
      .to(track.value, { x: -distance, duration: 0.9 }) // 卡 3 停留
  })
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  setup()
})

function killTween() {
  tween?.scrollTrigger?.kill()
  tween?.kill()
  tween = null
}

// 切语言后卡片文字长度会变，横向距离必须重算，直接重建整条 timeline
watch(locale, async () => {
  await nextTick()
  killTween()
  if (track.value) gsap.set(track.value, { x: 0 })
  setup()
})

onBeforeUnmount(() => {
  killTween()
})
</script>

<template>
  <section id="deploy" class="section deploy">
    <div ref="stage" class="stage">
      <div class="head">
        <div class="container">
          <div class="section-head" data-section-head>
            <span class="eyebrow">{{ t('deploy.eyebrow') }}</span>
            <h2>{{ t('deploy.title') }}</h2>
            <p>{{ t('deploy.desc') }}</p>
          </div>
        </div>
      </div>

      <div ref="track" class="track" data-batch>
        <article v-for="d in content.deploy.cards" :key="d.title" class="dep">
          <h3>{{ d.title }}</h3>
          <p class="desc">{{ d.desc }}</p>
          <CodeWindow :code="d.code" :lang="d.lang" />
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.deploy {
  position: relative;
  padding: 0;
  background: var(--deploy-bg);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  scroll-margin-top: -24px;
}

.stage {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.head {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2;
  padding: 24px 0 0;
  pointer-events: none;
}

.head .container {
  pointer-events: auto;
}

.track {
  display: flex;
  gap: 24px;
  padding: 0 24px;
  padding-top: 220px;
  padding-bottom: 48px;
  height: 100%;
  align-items: stretch;
  will-change: transform;
}

.dep {
  /* 每张卡宽度 = 视口 ~60%，让 track 自然比 viewport 宽出 ~2 张卡的滚动距离 */
  flex: 0 0 min(560px, calc(60vw - 24px));
  max-width: 600px;
  min-width: 320px;
  padding: 24px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
}

.dep h3 {
  font-size: 17px;
}

.dep .desc {
  margin: 10px 0 16px;
  font-size: 14.5px;
  color: var(--muted);
}

/* 移动端取消横向滚动，回到垂直堆叠 */
@media (max-width: 900px) {
  .stage {
    height: auto;
    overflow: visible;
  }
  .head {
    position: relative;
    padding: 64px 0 24px;
  }
  .track {
    flex-direction: column;
    padding: 0 24px 48px;
    height: auto;
    transform: none !important;
  }
  .dep {
    flex: 0 0 auto;
    max-width: none;
  }
}
</style>