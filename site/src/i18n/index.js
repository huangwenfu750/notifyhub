import { ref, computed } from 'vue'
import zh from './messages/zh.js'
import en from './messages/en.js'

const STORAGE_KEY = 'notifyhub-site-locale'
const messages = { zh, en }

export const DEFAULT_LOCALE = 'zh'

export const locales = [
  { code: 'zh', label: '中文' },
  { code: 'en', label: 'EN' },
]

function detectLocale() {
  // 默认中文；只有用户显式切换过才跟随 localStorage
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved && messages[saved]) return saved
  } catch {
    // localStorage 不可用（隐私模式等），回落默认语言
  }
  return DEFAULT_LOCALE
}

export const locale = ref(detectLocale())

/** 当前语言的完整消息对象（模板里用 content.xxx，会自动 unwrap） */
export const content = computed(() => messages[locale.value])

/**
 * 文档数据量大（中英各一份），由 DocsView 按需动态 import，
 * 避免把整份文档塞进主包。这里不静态引入。
 */

/**
 * 按路径取文案，如 t('nav.features')。
 * 渲染期间读取了 locale.value，因此语言切换会自动触发重渲染。
 */
export function t(key) {
  const dict = messages[locale.value]
  const value = key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), dict)
  if (value == null) {
    const fallback = key
      .split('.')
      .reduce((o, k) => (o == null ? undefined : o[k]), messages[DEFAULT_LOCALE])
    return fallback != null ? fallback : key
  }
  return value
}

export function setLocale(code) {
  if (!messages[code] || code === locale.value) return
  locale.value = code
  try {
    localStorage.setItem(STORAGE_KEY, code)
  } catch {
    // 忽略写入失败
  }
  applyDocumentLang()
}

export function toggleLocale() {
  setLocale(locale.value === 'zh' ? 'en' : 'zh')
}

export function applyDocumentLang() {
  if (typeof document === 'undefined') return
  document.documentElement.lang = locale.value === 'zh' ? 'zh-CN' : 'en'
}
