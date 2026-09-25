<script setup>
import { ref } from 'vue'
import { t, content } from '../i18n/index.js'

const copied = ref('')

async function copy(row) {
  try {
    await navigator.clipboard.writeText(row.cmd)
    copied.value = row.lang
    setTimeout(() => {
      if (copied.value === row.lang) copied.value = ''
    }, 1600)
  } catch {
    copied.value = ''
  }
}
</script>

<template>
  <section id="install" class="section">
    <div class="container">
      <div class="section-head" data-section-head>
        <span class="eyebrow">{{ t('install.eyebrow') }}</span>
        <h2>{{ t('install.title') }}</h2>
        <p>{{ t('install.desc') }}</p>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t('install.th.lang') }}</th>
              <th>{{ t('install.th.cmd') }}</th>
              <th>{{ t('install.th.source') }}</th>
            </tr>
          </thead>
          <tbody data-stagger-list>
            <tr v-for="r in content.install.rows" :key="r.lang">
              <td class="lang">{{ r.lang }}</td>
              <td>
                <div class="cmd">
                  <code class="mono">{{ r.cmd }}</code>
                  <button class="copy" :title="`${r.lang} · ${t('install.copyHint')}`" @click="copy(r)">
                    <svg v-if="copied !== r.lang" viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                      <rect x="9" y="9" width="11" height="11" rx="2" />
                      <path d="M5 15V5a2 2 0 0 1 2-2h8" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  </button>
                </div>
              </td>
              <td class="note">{{ r.note }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="foot">
        {{ t('install.releasesNote') }}
        <a :href="content.meta.releases" target="_blank" rel="noopener">{{ t('install.releasesLink') }}</a>
      </p>
    </div>
  </section>
</template>

<style scoped>
.table-wrap {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14.5px;
}

th {
  text-align: left;
  font-weight: 500;
  font-size: 12.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted-2);
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--overlay);
}

td {
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

tr:last-child td {
  border-bottom: 0;
}

.lang {
  color: var(--text);
  white-space: nowrap;
  width: 130px;
}

.note {
  color: var(--muted-2);
  white-space: nowrap;
  width: 150px;
}

.cmd {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cmd code {
  font-size: 13px;
  color: var(--code-accent);
  overflow-x: auto;
  white-space: nowrap;
}

.copy {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--overlay);
  color: var(--muted);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;
}

.copy:hover {
  color: var(--brand);
  border-color: var(--brand-line);
}

.foot {
  margin-top: 18px;
  font-size: 14px;
  color: var(--muted-2);
}

.foot a {
  color: var(--brand);
}

@media (max-width: 760px) {
  .note {
    display: none;
  }
  .lang {
    width: auto;
  }
}
</style>
