<script setup>
import { computed, ref, watch } from 'vue'
import DocBlocks from '../components/DocBlocks.vue'
import { locale } from '../i18n/index.js'
import { navigate, docSlug, route } from '../router.js'

const SLUGS = ['usage', 'config', 'protocol']

// 文档数据按需加载：中英各一份，只在真的进入 /docs 时才下载
const data = ref(null)
watch(
  locale,
  async (loc) => {
    const mod = loc === 'zh' ? await import('../i18n/docs/zh.js') : await import('../i18n/docs/en.js')
    data.value = mod.default
  },
  { immediate: true }
)

const slug = computed(() => docSlug(route.value))
const current = computed(() => (slug.value && data.value ? data.value[slug.value] : null))
const index = computed(() => data.value?.index)
const nav = computed(() => data.value?.nav || {})

// 章节跳转走 JS 滚动：hash 已被路由占用，不能再用 #id 锚点
function goSection(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function go(path) {
  navigate(path)
  window.scrollTo({ top: 0, behavior: 'auto' })
}

watch(slug, () => window.scrollTo({ top: 0, behavior: 'auto' }))
</script>

<template>
  <main class="docs">
    <div class="container layout">
      <aside class="sidebar">
        <nav v-if="data">
          <a class="side-top" :class="{ on: !slug }" href="#/docs" @click="go('/docs')">
            {{ nav.overview }}
          </a>

          <div v-for="s in SLUGS" :key="s" class="side-group">
            <a class="side-doc" :class="{ on: slug === s }" :href="`#/docs/${s}`" @click="go(`/docs/${s}`)">
              {{ data[s].title }}
            </a>
            <ul v-if="slug === s" class="side-sections">
              <li v-for="sec in data[s].sections" :key="sec.id">
                <a href="#" @click.prevent="goSection(sec.id)">{{ sec.title }}</a>
              </li>
            </ul>
          </div>
        </nav>
      </aside>

      <article class="content">
        <div v-if="!data" class="loading">
          <span class="dot"></span>
        </div>

        <div v-else-if="!current" class="index-view">
          <header class="doc-head">
            <span class="eyebrow">{{ nav.docs }}</span>
            <h1>{{ index.title }}</h1>
            <p class="lead">{{ index.lead }}</p>
          </header>

          <div class="cards">
            <a
              v-for="c in index.cards"
              :key="c.slug"
              class="doc-card"
              :href="`#/docs/${c.slug}`"
              @click="go(`/docs/${c.slug}`)"
            >
              <h2>{{ c.title }}</h2>
              <p>{{ c.desc }}</p>
              <span class="go">{{ nav.docs }} →</span>
            </a>
          </div>

          <section class="external">
            <h2>{{ index.externalTitle }}</h2>
            <ul>
              <li v-for="e in index.external" :key="e.href">
                <a :href="e.href" target="_blank" rel="noopener">{{ e.text }}</a>
              </li>
            </ul>
          </section>
        </div>

        <div v-else class="doc-view">
          <header class="doc-head">
            <span class="eyebrow">{{ nav.docs }}</span>
            <h1>{{ current.title }}</h1>
            <p class="lead">{{ current.desc }}</p>
          </header>

          <section v-for="sec in current.sections" :id="sec.id" :key="sec.id" class="doc-section">
            <h2>{{ sec.title }}</h2>
            <DocBlocks :blocks="sec.blocks" />
          </section>
        </div>
      </article>
    </div>
  </main>
</template>

<style scoped>
.docs {
  padding: 84px 0 96px;
}

.layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 48px;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: 84px;
  max-height: calc(100vh - 120px);
  overflow-y: auto;
}

.side-top {
  display: block;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--muted);
  padding: 8px 12px;
  border-radius: 8px;
  margin-bottom: 6px;
}

.side-group {
  margin-bottom: 4px;
}

.side-doc {
  display: block;
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text);
  padding: 8px 12px;
  border-radius: 8px;
}

.side-top:hover,
.side-doc:hover {
  background: var(--overlay-hover);
}

.side-top.on,
.side-doc.on {
  color: var(--brand);
  background: var(--brand-soft);
}

.side-sections {
  list-style: none;
  margin: 2px 0 10px;
  padding: 0 0 0 14px;
  border-left: 1px solid var(--border);
  margin-left: 14px;
}

.side-sections li a {
  display: block;
  font-size: 13.5px;
  color: var(--muted-2);
  padding: 5px 10px;
  border-radius: 6px;
}

.side-sections li a:hover {
  color: var(--text);
  background: var(--overlay);
}

.content {
  min-width: 0;
}

.doc-head {
  margin-bottom: 36px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}

.eyebrow {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 12px;
}

h1 {
  font-size: clamp(28px, 3.6vw, 38px);
  letter-spacing: -0.02em;
}

.lead {
  margin-top: 14px;
  font-size: 16px;
  color: var(--muted);
  max-width: 720px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 40px;
}

.doc-card {
  display: block;
  padding: 24px;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.doc-card:hover {
  border-color: var(--brand-line);
  transform: translateY(-2px);
}

.doc-card h2 {
  font-size: 17px;
  font-weight: 600;
}

.doc-card p {
  margin-top: 10px;
  font-size: 14.5px;
  color: var(--muted);
}

.go {
  display: inline-block;
  margin-top: 16px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--brand);
}

.external h2 {
  font-size: 16px;
  margin-bottom: 12px;
}

.external ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.external a {
  display: inline-block;
  font-size: 13.5px;
  padding: 6px 12px;
  border-radius: 8px;
  color: var(--muted);
  border: 1px solid var(--border);
  background: var(--overlay);
}

.external a:hover {
  color: var(--brand);
  border-color: var(--brand-line);
}

.loading {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.loading .dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid var(--border);
  border-top-color: var(--brand);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.doc-section {
  scroll-margin-top: 84px;
  margin-bottom: 48px;
}

.doc-section h2 {
  font-size: 21px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-top: 8px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  .sidebar {
    position: static;
    max-height: none;
  }
  .side-sections {
    display: none;
  }
}
</style>
