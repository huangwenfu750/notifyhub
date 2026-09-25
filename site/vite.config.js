import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// base 用相对路径：既能部署到 https://<user>.github.io/notifyhub/ 这类子路径，
// 也能挂到任意自定义域名根目录，无需重新构建。
export default defineConfig({
  base: './',
  plugins: [vue()],
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
  },
})
