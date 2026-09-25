import { onMounted, onBeforeUnmount } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

/**
 * 把组件里挂了 data-reveal / data-reveal-stagger 的节点，统一注册 GSAP + ScrollTrigger 入场动画。
 * 用 data-stagger-children 把容器里所有直接子节点视为一组做错位入场。
 * 节点 SSR/CSR 都安全：未挂载就什么都不做。
 */
export function useReveal() {
  let ctx

  onMounted(() => {
    ctx = gsap.context(() => {
      // Hero 文字与按钮错位入场（页面加载即跑，不绑定滚动）
      gsap.from('[data-hero] > *', {
        y: 24,
        opacity: 0,
        duration: 0.7,
        stagger: 0.08,
        ease: 'power2.out',
      })

      // 顶部徽章与 chips：更细更密的 stagger
      gsap.from('[data-hero-stagger] > *', {
        y: 14,
        opacity: 0,
        duration: 0.5,
        stagger: 0.05,
        delay: 0.15,
        ease: 'power2.out',
      })

      // Hero 终端窗口从右侧滑入
      gsap.from('[data-hero-terminal]', {
        y: 30,
        opacity: 0,
        duration: 0.9,
        delay: 0.35,
        ease: 'power3.out',
      })

      // 各 section 标题 + 副标淡入
      gsap.utils.toArray('[data-section-head]').forEach((head) => {
        gsap.from(head.children, {
          y: 24,
          opacity: 0,
          duration: 0.7,
          stagger: 0.08,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: head,
            start: 'top 85%',
            once: true,
          },
        })
      })

      // 卡片批量入场：特性 / 部署
      gsap.utils.toArray('[data-batch]').forEach((batch) => {
        gsap.from(batch.children, {
          y: 32,
          opacity: 0,
          duration: 0.7,
          stagger: 0.08,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: batch,
            start: 'top 88%',
            once: true,
          },
        })
      })

      // 列表错位入场：SDK tab、安装表、quick start
      gsap.utils.toArray('[data-stagger-list]').forEach((list) => {
        gsap.from(list.children, {
          y: 18,
          opacity: 0,
          duration: 0.6,
          stagger: 0.08,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: list,
            start: 'top 88%',
            once: true,
          },
        })
      })

      // Architecture 节点连线后元素逐项出现
      gsap.utils.toArray('[data-arch-side]').forEach((side) => {
        gsap.from(side.children, {
          x: side.dataset.archSide === 'right' ? 18 : -18,
          opacity: 0,
          duration: 0.6,
          stagger: 0.06,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: side,
            start: 'top 88%',
            once: true,
          },
        })
      })

      // Server 框呼吸光环（无限循环）
      gsap.to('[data-server-glow]', {
        boxShadow: '0 0 60px 0 rgba(91, 141, 239, 0.45)',
        duration: 2.4,
        yoyo: true,
        repeat: -1,
        ease: 'sine.inOut',
      })

      // 终端逐行打字
      gsap.utils.toArray('[data-typing]').forEach((box) => {
        const lines = box.querySelectorAll('[data-typing-line]')
        if (!lines.length) return
        gsap.from(lines, {
          y: 6,
          opacity: 0,
          duration: 0.4,
          stagger: 0.32,
          ease: 'power2.out',
          delay: 0.45,
        })
      })
    })

    // 用户系统设置 prefers-reduced-motion：跳过入场动画
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      gsap.globalTimeline.clear()
    }
  })

  onBeforeUnmount(() => {
    if (ctx) ctx.revert()
  })
}