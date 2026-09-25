<script setup>
import { defineAsyncComponent } from 'vue'
import CodeWindow from './CodeWindow.vue'
import { t, content } from '../i18n/index.js'

// Three.js 体积大，单独拆 chunk，首屏文字先渲染，canvas 随后补上
const HeroCanvas = defineAsyncComponent(() => import('./HeroCanvas.vue'))
</script>

<template>
  <section id="top" class="hero">
    <HeroCanvas />
    <div class="glow" aria-hidden="true"></div>
    <div class="grid-bg" aria-hidden="true"></div>

    <div class="container hero-inner">
      <div class="copy" data-hero>
        <div class="badges" data-hero-stagger>
          <span class="badge">{{ t('hero.badgeVersion') }}</span>
          <span class="badge">{{ t('hero.badgeLicense') }}</span>
          <span class="badge">{{ t('hero.badgeProto') }}</span>
        </div>

        <h1>{{ t('hero.title') }}</h1>

        <p class="lead">{{ t('hero.lead') }}</p>

        <div class="actions">
          <a class="btn btn-primary" href="#start">{{ t('hero.ctaStart') }}</a>
          <a class="btn btn-ghost" :href="content.meta.repo" target="_blank" rel="noopener">
            {{ t('hero.ctaGithub') }}
          </a>
        </div>

        <div class="platforms" data-hero-stagger>
          <span class="label">{{ t('hero.platformsLabel') }}</span>
          <span v-for="p in content.arch.platforms" :key="p" class="chip">{{ p }}</span>
        </div>
      </div>

      <div class="visual" data-hero-terminal>
        <CodeWindow
          :title="t('hero.terminalTitle')"
          :code="content.hero.terminal"
          lang="bash"
          typing
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  overflow: hidden;
  padding: 76px 0 96px;
  border-bottom: 1px solid var(--border);
  background: var(--hero-bg);
  scroll-margin-top: -76px;
}

.glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(760px 380px at 78% -6%, var(--glow-1), transparent 70%),
    radial-gradient(680px 340px at 8% -2%, var(--glow-2), transparent 70%);
  pointer-events: none;
  z-index: 1;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(var(--grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-line) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(900px 460px at 50% 0%, #000 40%, transparent 100%);
  pointer-events: none;
  z-index: 2;
}

.hero-inner {
  position: relative;
  z-index: 3;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  gap: 56px;
  align-items: center;
}

.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.badge {
  font-family: var(--font-mono);
  font-size: 12.5px;
  padding: 4px 11px;
  border-radius: 999px;
  color: var(--brand);
  border: 1px solid var(--brand-line);
  background: var(--brand-soft);
}

h1 {
  font-size: clamp(34px, 5.2vw, 56px);
  letter-spacing: -0.03em;
  /* background-clip:text 在某些渲染组合下会让文本完全透明，
     改用纯色 + brand 强调段，保持两套主题下都可见可读 */
  color: var(--text);
}

.lead {
  margin-top: 20px;
  max-width: 620px;
  font-size: 17px;
  color: var(--muted);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
}

.platforms {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 30px;
}

.label {
  font-size: 13px;
  color: var(--muted);
  margin-right: 4px;
}

.chip {
  font-size: 13px;
  padding: 5px 12px;
  border-radius: 8px;
  color: var(--muted);
  border: 1px solid var(--border);
  background: var(--overlay);
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease;
  animation: chipFloat 4.4s ease-in-out infinite;
}

.chip:nth-child(2) {
  animation-delay: 0.2s;
}
.chip:nth-child(3) {
  animation-delay: 0.6s;
}
.chip:nth-child(4) {
  animation-delay: 1.0s;
}

.chip:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

@keyframes chipFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chip {
    animation: none;
  }
}

@media (max-width: 900px) {
  .hero-inner {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  .hero {
    padding: 56px 0 72px;
  }
}
</style>
