<script setup>
import { computed } from 'vue'
import { highlight } from '../utils/highlight.js'

const props = defineProps({
  title: { type: String, default: '' },
  code: { type: String, required: true },
  lang: { type: String, default: 'bash' },
  typing: { type: Boolean, default: false },
})

const html = computed(() => highlight(props.code, props.lang))

const lines = computed(() =>
  props.typing ? props.code.split('\n') : null
)
</script>

<template>
  <div class="window">
    <div class="bar">
      <span class="dots">
        <i /><i /><i />
      </span>
      <span class="title mono">{{ title }}</span>
    </div>
    <pre v-if="!typing" class="code"><code v-html="html"></code></pre>
    <pre v-else class="code typing" data-typing>
<code><span
  v-for="(l, i) in lines"
  :key="i"
  class="line"
  data-typing-line
  v-html="highlight(l, lang) || '&nbsp;'"
></span></code></pre>
  </div>
</template>

<style scoped>
.window {
  background: var(--code-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--card-shadow);
}

.bar {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 38px;
  padding: 0 14px;
  background: var(--overlay);
  border-bottom: 1px solid var(--border);
}

.dots {
  display: inline-flex;
  gap: 6px;
}

.dots i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--dot);
}

.title {
  font-size: 12.5px;
  color: var(--muted-2);
}

.code {
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 18px 20px;
}

.typing .line {
  display: block;
  min-height: 1.6em;
}
</style>
