<script setup>
import CodeWindow from './CodeWindow.vue'
import { t, content } from '../i18n/index.js'
</script>

<template>
  <section id="start" class="section alt">
    <div class="container">
      <div class="section-head" data-section-head>
        <span class="eyebrow">{{ t('start.eyebrow') }}</span>
        <h2>{{ t('start.title') }}</h2>
        <p>{{ t('start.desc') }}</p>
      </div>

      <div class="steps" data-stagger-list>
        <div v-for="s in content.start.steps" :key="s.n" class="step">
          <div class="text">
            <span class="num mono">{{ s.n }}</span>
            <h3>{{ s.title }}</h3>
            <p>{{ s.desc }}</p>
          </div>
          <CodeWindow :title="s.title" :code="s.code" :lang="s.lang" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.alt {
  background: var(--alt-bg);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.step {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 32px;
  align-items: center;
  padding: 26px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
}

.num {
  display: inline-block;
  font-size: 13px;
  color: var(--brand);
  letter-spacing: 0.12em;
}

h3 {
  margin-top: 8px;
  font-size: 19px;
}

.text p {
  margin-top: 10px;
  font-size: 14.5px;
  color: var(--muted);
}

@media (max-width: 900px) {
  .step {
    grid-template-columns: 1fr;
    gap: 18px;
  }
}
</style>
