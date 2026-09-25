<script setup>
import CodeWindow from './CodeWindow.vue'

defineProps({
  blocks: { type: Array, required: true },
})
</script>

<template>
  <div class="blocks">
    <template v-for="(b, i) in blocks" :key="i">
      <p v-if="b.type === 'p'" class="para">{{ b.text }}</p>

      <h3 v-else-if="b.type === 'h3'" class="sub">{{ b.text }}</h3>

      <CodeWindow v-else-if="b.type === 'code'" :code="b.code" :lang="b.lang" />

      <div v-else-if="b.type === 'table'" class="tw">
        <table>
          <thead>
            <tr>
              <th v-for="h in b.head" :key="h">{{ h }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in b.rows" :key="ri">
              <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <ol v-else-if="b.type === 'list' && b.ordered" class="lst">
        <li v-for="(it, li) in b.items" :key="li">{{ it }}</li>
      </ol>

      <ul v-else-if="b.type === 'list'" class="lst">
        <li v-for="(it, li) in b.items" :key="li">{{ it }}</li>
      </ul>

      <p v-else-if="b.type === 'note'" class="note">{{ b.text }}</p>

      <pre v-else-if="b.type === 'text'" class="plain">{{ b.text }}</pre>
    </template>
  </div>
</template>

<style scoped>
.para {
  margin: 0 0 16px;
  font-size: 15px;
  color: var(--muted);
  line-height: 1.8;
}

.sub {
  margin: 30px 0 14px;
  font-size: 16.5px;
  font-weight: 600;
  color: var(--text);
}

.sub:first-child {
  margin-top: 0;
}

.tw {
  margin: 0 0 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th {
  text-align: left;
  font-weight: 500;
  font-size: 12.5px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--muted-2);
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--overlay);
  white-space: nowrap;
}

td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  color: var(--muted);
  vertical-align: top;
}

tr:last-child td {
  border-bottom: 0;
}

td:first-child {
  color: var(--text);
  white-space: nowrap;
}

.lst {
  margin: 0 0 18px;
  padding-left: 22px;
  font-size: 15px;
  color: var(--muted);
  line-height: 1.85;
}

.lst li {
  margin-bottom: 6px;
}

.lst li::marker {
  color: var(--brand);
}

.note {
  margin: 0 0 18px;
  padding: 12px 16px;
  border-left: 3px solid var(--brand);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: var(--brand-soft);
  font-size: 14.5px;
  color: var(--text);
}

.plain {
  margin: 0 0 18px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--muted);
}
</style>
