<script setup>
import { t, content } from '../i18n/index.js'
</script>

<template>
  <section id="features" class="section">
    <div class="container">
      <div class="section-head" data-section-head>
        <span class="eyebrow">{{ t('features.eyebrow') }}</span>
        <h2>{{ t('features.title') }}</h2>
        <p>{{ t('features.desc') }}</p>
      </div>

      <div class="grid" data-batch>
        <article v-for="f in content.features.items" :key="f.title" class="feat">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" v-html="f.icon"></svg>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
          <div class="tags">
            <span v-for="t in f.tags" :key="t">{{ t }}</span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.feat {
  padding: 26px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.feat:hover {
  border-color: var(--brand-line);
  transform: translateY(-2px);
}

.icon {
  width: 26px;
  height: 26px;
  color: var(--brand);
  margin-bottom: 16px;
}

h3 {
  font-size: 17px;
  font-weight: 600;
}

p {
  margin-top: 10px;
  font-size: 14.5px;
  color: var(--muted);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
}

.tags span {
  font-family: var(--font-mono);
  font-size: 11.5px;
  padding: 3px 9px;
  border-radius: 6px;
  color: var(--muted-2);
  background: var(--overlay);
  border: 1px solid var(--border);
}

@media (max-width: 940px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
