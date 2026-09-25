import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'notifyhub-site-theme'

/** 'system' | 'light' | 'dark'，默认跟随系统 */
export const modes = ['system', 'light', 'dark']

function readStored() {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    return modes.includes(v) ? v : 'system'
  } catch {
    return 'system'
  }
}

export const mode = ref(readStored())

const query = typeof window !== 'undefined' && window.matchMedia
  ? window.matchMedia('(prefers-color-scheme: dark)')
  : null

export const systemPrefersDark = ref(query ? query.matches : true)

if (query) {
  const onChange = (e) => {
    systemPrefersDark.value = e.matches
  }
  // Safari < 14 只有 addListener
  if (query.addEventListener) query.addEventListener('change', onChange)
  else if (query.addListener) query.addListener(onChange)
}

/** 实际生效的主题：'light' | 'dark' */
export const resolved = computed(() =>
  mode.value === 'system' ? (systemPrefersDark.value ? 'dark' : 'light') : mode.value
)

export function applyTheme() {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = resolved.value
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.content = resolved.value === 'dark' ? '#0b1020' : '#ffffff'
}

watch(resolved, applyTheme)

export function setMode(next) {
  if (!modes.includes(next) || next === mode.value) return
  mode.value = next
  try {
    localStorage.setItem(STORAGE_KEY, next)
  } catch {
    // 隐私模式等场景忽略写入失败
  }
}

/** 点一下切到下一档：跟随系统 → 日间 → 夜间 → 跟随系统 */
export function cycleMode() {
  const i = modes.indexOf(mode.value)
  setMode(modes[(i + 1) % modes.length])
}
