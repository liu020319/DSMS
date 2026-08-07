<template>
  <router-view />
</template>

<script setup>
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Microsoft YaHei', sans-serif;
}
* { box-sizing: border-box; }
body { color: #273c32; background: #f4f7f5; }
button, input, textarea, select { font-family: inherit; }
@media (max-width: 760px) {
  .el-dialog { width: calc(100% - 24px) !important; margin-top: 6vh !important; }
  .el-message-box { width: calc(100% - 28px) !important; }
  .el-card__header { padding: 15px 16px; }
  .el-card__body { padding: 16px; }
}
</style>
