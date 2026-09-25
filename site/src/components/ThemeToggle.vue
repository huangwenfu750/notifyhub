<script setup>
import { computed } from 'vue'
import { mode, cycleMode } from '../theme.js'
import { t } from '../i18n/index.js'

const ICONS = {
  system: '<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/>',
  light:
    '<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="M4.9 4.9l1.4 1.4"/><path d="M17.7 17.7l1.4 1.4"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="M4.9 19.1l1.4-1.4"/><path d="M17.7 6.3l1.4-1.4"/>',
  dark: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
}

// 图标表示的是「当前档位」：system / light / dark
const label = computed(() => t(`theme.${mode.value}`))
const title = computed(() => `${t('theme.label')}：${label.value}（${t('theme.hint')}）`)
</script>

<template>
  <button class="theme-toggle" :title="title" :aria-label="title" @click="cycleMode">
    <svg
      viewBox="0 0 24 24"
      width="15"
      height="15"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      v-html="ICONS[mode]"
    ></svg>
  </button>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid var(--border-strong);
  background: var(--overlay);
  color: var(--muted);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease, background 0.15s ease;
}

.theme-toggle:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}
</style>
