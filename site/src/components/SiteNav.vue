<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ThemeToggle from './ThemeToggle.vue'
import { t, content, locale, toggleLocale } from '../i18n/index.js'
import { route, isDocsPath } from '../router.js'

const links = computed(() => [
  { href: '#features', text: t('nav.features') },
  { href: '#sdk', text: t('nav.sdk') },
  { href: '#start', text: t('nav.start') },
  { href: '#deploy', text: t('nav.deploy') },
  { href: '#/docs', text: t('nav.docs'), router: true },
])

const onDocs = computed(() => isDocsPath(route.value))

// 按钮上显示的是「切过去的那门语言」
const switchLabel = computed(() => (locale.value === 'zh' ? 'EN' : '中文'))
const switchTitle = computed(() => t('lang.switchTo'))

const scrolled = ref(false)

const onScroll = () => {
  scrolled.value = window.scrollY > 12
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="nav" :class="{ scrolled }">
    <div class="container nav-inner">
      <a class="brand" href="#top">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" />
          <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
        <span>NotifyHub</span>
      </a>

      <nav class="links">
        <a
          v-for="l in links"
          :key="l.href"
          :href="l.href"
          :class="{ on: l.router && onDocs }"
        >{{ l.text }}</a>
      </nav>

      <button class="lang-switch" :title="switchTitle" :aria-label="switchTitle" @click="toggleLocale">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="12" cy="12" r="9" />
          <path d="M3 12h18" />
          <path d="M12 3a15 15 0 0 1 0 18a15 15 0 0 1 0-18" />
        </svg>
        {{ switchLabel }}
      </button>

      <ThemeToggle />

      <a class="btn btn-ghost nav-cta" :href="content.meta.repo" target="_blank" rel="noopener">
        <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor" aria-hidden="true">
          <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.07-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.15 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A7.99 7.99 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
        </svg>
        {{ t('nav.github') }}
      </a>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 50;
  border-bottom: 1px solid transparent;
  transition: background 0.2s ease, border-color 0.2s ease, backdrop-filter 0.2s ease;
}

.nav.scrolled {
  background: var(--nav-bg);
  backdrop-filter: blur(12px);
  border-bottom-color: var(--border);
}

.nav-inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 66px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 17px;
  letter-spacing: -0.01em;
  color: var(--text);
}

.brand svg {
  color: var(--brand);
}

.links {
  display: flex;
  gap: 26px;
  margin-left: auto;
  font-size: 14.5px;
  color: var(--muted);
}

.links a:hover {
  color: var(--text);
}

.links a.on {
  color: var(--brand);
}

.nav-cta {
  height: 38px;
  padding: 0 16px;
  font-size: 14px;
}

.lang-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: var(--overlay);
  color: var(--muted);
  font-family: var(--font-sans);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.lang-switch:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

@media (max-width: 820px) {
  .links {
    display: none;
  }
  .lang-switch {
    margin-left: auto;
  }
}
</style>
