import { createApp } from 'vue'
import {
  ElAlert, ElButton, ElDatePicker, ElDialog, ElDrawer, ElEmpty, ElForm, ElFormItem,
  ElInput, ElInputNumber, ElOption, ElSegmented, ElSelect, ElTable, ElTableColumn
} from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles.css'

const app = createApp(App)
;[
  ElAlert, ElButton, ElDatePicker, ElDialog, ElDrawer, ElEmpty, ElForm, ElFormItem,
  ElInput, ElInputNumber, ElOption, ElSegmented, ElSelect, ElTable, ElTableColumn
].forEach(component => app.component(component.name, component))
app.use(router).mount('#app')
