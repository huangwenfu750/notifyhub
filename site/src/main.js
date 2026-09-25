import { createApp } from 'vue'
import App from './App.vue'
import { applyDocumentLang } from './i18n/index.js'
import { applyTheme } from './theme.js'
import './styles.css'

applyTheme()
applyDocumentLang()
createApp(App).mount('#app')
