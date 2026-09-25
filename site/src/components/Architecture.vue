<script setup>
import { t, content } from '../i18n/index.js'
</script>

<template>
  <section id="architecture" class="section">
    <div class="container">
      <div class="section-head" data-section-head>
        <span class="eyebrow">{{ t('arch.eyebrow') }}</span>
        <h2>{{ t('arch.title') }}</h2>
        <p>{{ t('arch.desc') }}</p>
      </div>

      <div class="diagram">
        <div class="col" data-arch-side="left">
          <div class="col-title">{{ t('arch.clientTitle') }}</div>
          <div class="nodes">
            <span v-for="c in content.arch.clients" :key="c" class="node">{{ c }}</span>
          </div>
        </div>

        <div class="arrow">
          <span class="line"></span>
          <span class="tag mono">{{ t('arch.tagGrpc') }}</span>
          <span class="line"></span>
        </div>

        <div class="col server" data-server-glow>
          <div class="col-title">{{ t('arch.serverTitle') }}</div>
          <div class="rpcs">
            <div v-for="r in content.arch.rpcs" :key="r.name" class="rpc">
              <span class="rpc-name mono">{{ r.name }}</span>
              <span class="rpc-desc">{{ r.desc }}</span>
            </div>
          </div>
          <div class="internals">
            <span v-for="i in content.arch.internals" :key="i">{{ i }}</span>
          </div>
        </div>

        <div class="arrow">
          <span class="line"></span>
          <span class="tag mono">{{ t('arch.tagTopic') }}</span>
          <span class="line"></span>
        </div>

        <div class="col" data-arch-side="right">
          <div class="col-title">{{ t('arch.platformTitle') }}</div>
          <div class="nodes">
            <span v-for="p in content.arch.platforms" :key="p" class="node accent">{{ p }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.diagram {
  display: grid;
  grid-template-columns: 1fr 130px 1.35fr 130px 1fr;
  align-items: stretch;
  gap: 0;
  padding: 26px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: linear-gradient(180deg, var(--panel), var(--bg-soft));
}

.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.col-title {
  font-size: 12.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted-2);
}

.nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.node {
  padding: 9px 13px;
  border-radius: 9px;
  font-size: 13.5px;
  color: var(--muted);
  border: 1px solid var(--border);
  background: var(--overlay);
}

.node.accent {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.server {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--brand-2);
  background: var(--server-bg);
}

.rpcs {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rpc {
  display: flex;
  flex-direction: column;
  padding: 9px 12px;
  border-radius: 9px;
  background: var(--rpc-bg);
  border: 1px solid var(--border);
}

.rpc-name {
  font-size: 13px;
  color: var(--rpc-name);
}

.rpc-desc {
  font-size: 12.5px;
  color: var(--muted-2);
}

.internals {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
  padding-top: 12px;
}

.internals span {
  font-size: 12px;
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--muted);
  border: 1px dashed var(--border-strong);
}

.arrow {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 8px;
}

.arrow .line {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--border-strong));
}

.arrow .line:last-child {
  background: linear-gradient(90deg, var(--border-strong), transparent);
}

.tag {
  font-size: 11.5px;
  color: var(--muted-2);
  white-space: nowrap;
}

@media (max-width: 940px) {
  .diagram {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .arrow {
    flex-direction: row;
    padding: 6px 0;
  }
  .arrow .line {
    height: 1px;
    width: 40px;
  }
}
</style>
