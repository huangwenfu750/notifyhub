<script setup>
import { computed } from 'vue'
import { t, content } from '../i18n/index.js'

const links = computed(() => [
  { text: t('footer.github'), href: content.value.meta.repo },
  { text: t('footer.releases'), href: content.value.meta.releases },
  { text: t('footer.docs'), href: content.value.meta.docs },
])
</script>

<template>
  <footer class="footer">
    <div class="container inner">
      <div class="brand">
        <span class="logo">NotifyHub</span>
        <span class="ver mono">v{{ content.meta.version }}</span>
      </div>
      <nav class="links">
        <a v-for="l in links" :key="l.text" :href="l.href" target="_blank" rel="noopener">
          {{ l.text }}
        </a>
      </nav>
      <span class="license">{{ t('footer.license') }}</span>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  border-top: 1px solid var(--border);
  padding: 34px 0;
  background: var(--footer-bg);
}

.inner {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.logo {
  font-weight: 700;
}

.ver {
  margin-left: 10px;
  font-size: 12.5px;
  color: var(--muted-2);
}

.links {
  display: flex;
  gap: 22px;
  margin-left: auto;
  font-size: 14px;
  color: var(--muted);
}

.links a:hover {
  color: var(--brand);
}

.license {
  font-size: 13px;
  color: var(--muted-2);
}
</style>
