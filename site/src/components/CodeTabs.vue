<script setup>
import { computed, ref } from 'vue'
import CodeWindow from './CodeWindow.vue'
import { t, content } from '../i18n/index.js'

const active = ref(content.value.sdk.tabs[0].id)
const current = computed(
  () => content.value.sdk.tabs.find((x) => x.id === active.value) || content.value.sdk.tabs[0]
)
</script>

<template>
  <section id="sdk" class="section">
    <div class="container">
      <div class="section-head" data-section-head>
        <span class="eyebrow">{{ t('sdk.eyebrow') }}</span>
        <h2>{{ t('sdk.title') }}</h2>
        <p>{{ t('sdk.desc') }}</p>
      </div>

      <div class="tabs" data-stagger-list>
        <button
          v-for="tab in content.sdk.tabs"
          :key="tab.id"
          class="tab"
          :class="{ on: active === tab.id }"
          @click="active = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="panel" data-batch>
        <div class="install">
          <span class="install-label">{{ t('sdk.installLabel') }}</span>
          <code class="mono">{{ current.install }}</code>
        </div>
        <CodeWindow :title="`${current.label} · publish`" :code="current.code" :lang="current.lang" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--overlay);
  width: fit-content;
  max-width: 100%;
}

.tab {
  border: 0;
  background: transparent;
  color: var(--muted);
  font-family: var(--font-sans);
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}

.tab:hover {
  color: var(--text);
}

.tab.on {
  color: var(--brand-ink);
  background: linear-gradient(120deg, var(--brand), #35b3ff);
  font-weight: 600;
}

.panel {
  margin-top: 18px;
}

.install {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.install-label {
  font-size: 12.5px;
  color: var(--muted-2);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.install code {
  font-size: 13.5px;
  color: var(--code-accent);
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  overflow-x: auto;
  max-width: 100%;
}
</style>
