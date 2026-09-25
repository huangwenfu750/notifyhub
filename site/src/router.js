import { ref } from 'vue'

export const HOME = '/'
export const DOCS = '/docs'

function normalize(hash) {
  const path = (hash || '').replace(/^#/, '')
  // #features 这类是首页锚点，不是路由路径（路由一律以 / 开头）
  if (!path || !path.startsWith('/')) return HOME
  return path
}

// 用 hash 路由：GitHub Pages 不做 SPA fallback，hash 是唯一零配置就能刷新/直达的方案
export const route = ref(normalize(window.location.hash))

window.addEventListener('hashchange', () => {
  route.value = normalize(window.location.hash)
})

export function navigate(path) {
  if (route.value === path) return
  window.location.hash = path
}

export function isDocsPath(path) {
  return path === DOCS || path.startsWith(DOCS + '/')
}

/** /docs/usage → 'usage'；/docs → '' */
export function docSlug(path) {
  if (!isDocsPath(path)) return ''
  const rest = path.slice(DOCS.length).replace(/^\//, '')
  return rest.split('/')[0] || ''
}

/** 打开文档页时滚到顶部（hash 里带 #section 锚点的除外） */
export function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'auto' })
}
