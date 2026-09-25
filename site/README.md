# NotifyHub 项目站点

Vue 3 + Vite 写的单页介绍站点，纯静态产物，部署到 GitHub Pages 后地址形如
`https://<用户名>.github.io/notifyhub/`。

## 本地开发

```bash
cd site
npm install
npm run dev       # 开发服务器
npm run build     # 产出 dist/
npm run preview   # 预览构建产物
```

## 部署

仓库已带 `.github/workflows/pages.yml`：推 `main` 且 `site/**` 有改动时自动构建并发布。

首次启用只需在 GitHub 仓库 **Settings → Pages → Source** 里选 **GitHub Actions**，
之后不再需要手工操作。若改用自定义域名，在 Pages 设置里填域名即可 ——
构建用的是相对路径 `base: './'`，两种访问方式都不需要重新构建。

## 结构

```
site/
├─ index.html
├─ vite.config.js
└─ src/
   ├─ App.vue
   ├─ main.js
   ├─ styles.css                 # 全局变量、按钮、代码块、卡片
   ├─ router.js                  # hash 路由（GitHub Pages 无 SPA fallback，只能用 hash）
   ├─ views/
   │  ├─ HomeView.vue            # 首页各 section
   │  └─ DocsView.vue            # 文档页（侧边栏 + 正文）
   ├─ components/DocBlocks.vue   # 把 docs 数据里的 block 渲染成段落/代码/表格/列表
   ├─ i18n/
   │  ├─ index.js                # locale ref / t() / setLocale() / localStorage 持久化
   │  ├─ messages/
   │  │  ├─ zh.js                # 中文文案 + 内容数据（默认语言）
   │  │  └─ en.js                # 英文文案 + 内容数据
   │  └─ docs/
   │     ├─ zh.js                # 三份文档的结构化数据（按需 import，不进主包）
   │     └─ en.js
   ├─ data/icons.js              # 语言无关的图标路径
   ├─ composables/useReveal.js   # GSAP 入场与滚动动画
   ├─ utils/highlight.js         # 轻量语法高亮，不引第三方库
   └─ components/
      ├─ SiteNav.vue / SiteFooter.vue
      ├─ SiteHero.vue            # 首屏 + 终端演示
      ├─ Architecture.vue        # 客户端 → gRPC → Server → 平台
      ├─ FeatureGrid.vue
      ├─ CodeTabs.vue            # 五种语言 SDK 切换
      ├─ QuickStart.vue
      ├─ DeployCards.vue         # 水平滚动揭示（GSAP pin）
      ├─ InstallTable.vue
      ├─ DecorLeafer.vue         # leafer-ui 装饰带
      ├─ HeroCanvas.vue          # Three.js 背景
      └─ CodeWindow.vue          # 带标题栏的代码窗口
```

## 文档页

顶部导航「文档」进入 `#/docs`。hash 路由是刻意的：GitHub Pages 不做 SPA fallback，
`/docs/usage` 这种路径刷新会 404，而 `#/docs/usage` 可以直接刷新和分享。

三份文档对应仓库里的 `docs/*.md`，内容结构化成 block 数据（段落 / 代码 / 表格 / 列表 / 提示），
由 `DocBlocks.vue` 渲染，代码块复用首页的高亮与窗口样式。

| 路由 | 内容 |
|---|---|
| `#/docs` | 文档首页：三份文档卡片 + 仓库原文链接 |
| `#/docs/usage` | 使用说明：启动、最小配置、核心概念、SDK、运行时管理、排错、已知边界 |
| `#/docs/config` | 配置手册：server / auth / defaults / platforms[] |
| `#/docs/protocol` | 协议参考：六个方法、鉴权、投递语义、渠道加签 |

文档数据（中英各 ~17KB）由 DocsView 动态 import，不进主包。
侧边栏章节跳转走 JS 滚动而不是锚点 —— hash 已被路由占用。

## 主题

右上角第二个按钮循环切换：**跟随系统 → 日间 → 夜间 → 跟随系统**（默认跟随系统）。
选择写入 `localStorage`（键 `notifyhub-site-theme`），并同步 `<meta name="theme-color">`。

实现在 `src/theme.js`：`mode`（三档）、`resolved`（实际生效的 light/dark）、`cycleMode()`。
两套 CSS 变量写在 `src/styles.css` 的 `:root` 与 `:root[data-theme='light']`。

`index.html` 与 `404.html` 顶部各有一段内联脚本，在首屏绘制前写好 `data-theme`——
没有它，深色用户会看到一瞬间的白屏。

**加新组件时注意**：颜色一律走变量，不要写死。canvas 不吃 CSS 变量，
所以 `HeroCanvas.vue`（Three.js 材质）和 `DecorLeafer.vue`（leafer 图形）
各有一份 `COLORS` 表，靠 `watch(resolved)` 切换。

## 多语言

默认中文，右上角按钮切换中 / EN，选择写入 `localStorage`（键 `notifyhub-site-locale`），
并同步 `<html lang>`。没有自动按浏览器语言切换——新访客一律中文。

改文案只需动 `src/i18n/messages/{zh,en}.js`：两份结构必须一致，
`t('a.b')` 按路径取值，取不到时回落到中文同名键。

发新版时要同步两份语言包里的 `meta.version` 与各处安装坐标。
