const KEYWORDS = {
  java: [
    'public', 'private', 'protected', 'static', 'final', 'void', 'class', 'interface',
    'package', 'import', 'new', 'try', 'catch', 'finally', 'throw', 'return', 'if',
    'else', 'for', 'while', 'var', 'null', 'true', 'false', 'this', 'extends',
    'implements', 'String', 'boolean', 'int',
  ],
  python: [
    'def', 'class', 'import', 'from', 'with', 'as', 'return', 'await', 'async', 'if',
    'elif', 'else', 'for', 'while', 'in', 'not', 'and', 'or', 'lambda', 'None', 'True',
    'False', 'try', 'except', 'raise', 'pass',
  ],
  ts: [
    'const', 'let', 'var', 'new', 'await', 'async', 'function', 'return', 'class',
    'import', 'from', 'export', 'interface', 'type', 'if', 'else', 'for', 'while',
    'try', 'catch', 'null', 'undefined', 'true', 'false', 'this', 'of',
  ],
  go: [
    'func', 'package', 'import', 'var', 'const', 'type', 'struct', 'interface',
    'range', 'defer', 'go', 'chan', 'map', 'return', 'if', 'else', 'for', 'nil',
    'true', 'false', 'err', 'ctx',
  ],
  bash: ['cd', 'cp', 'sudo', 'if', 'then', 'fi', 'export'],
  yaml: ['true', 'false', 'null'],
}

const HASH_COMMENT = new Set(['bash', 'yaml', 'python', 'sh'])

function escapeHtml(str) {
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/**
 * 极轻量的语法高亮：只做词法着色，不引第三方库，产物体积不变。
 * 只转义 & < >，保留引号，方便用正则直接匹配字符串字面量。
 */
export function highlight(code, lang = 'ts') {
  const kw = KEYWORDS[lang] || KEYWORDS.ts
  const comment = HASH_COMMENT.has(lang) ? '#[^\\n]*' : '//[^\\n]*'
  const string = '"(?:[^"\\\\]|\\\\.)*"|\'(?:[^\'\\\\]|\\\\.)*\''
  const key = lang === 'yaml' ? '^\\s*[\\w.\\-]+(?=\\s*:)' : '(?!)'
  const pattern = new RegExp(
    `(${comment})|(${string})|(${key})|(\\b(?:${kw.join('|')})\\b)|(\\b[A-Z][A-Za-z0-9_]{2,}\\b)|(\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\())|(\\b\\d+(?:\\.\\d+)?\\b)`,
    'gm'
  )

  return escapeHtml(code).replace(
    pattern,
    (m, c, s, k, w, t, f, n) => {
      if (c) return `<span class="tok-com">${c}</span>`
      if (s) return `<span class="tok-str">${s}</span>`
      if (k) return `<span class="tok-fn">${k}</span>`
      if (w) return `<span class="tok-key">${w}</span>`
      if (t) return `<span class="tok-type">${t}</span>`
      if (f) return `<span class="tok-fn">${f}</span>`
      if (n) return `<span class="tok-num">${n}</span>`
      return m
    }
  )
}
