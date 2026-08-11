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
  /* 手机输入框小于 16px 时，iOS/部分安卓浏览器会自动放大页面。 */
  .el-input__inner, .el-textarea__inner, .el-select__wrapper, .el-input-number input {
    font-size: 16px !important;
  }
  button, .el-button, .el-input__wrapper, .el-select__wrapper { touch-action: manipulation; }
  .el-overlay-dialog {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    padding: 12px;
  }
  .el-dialog {
    width: 100% !important;
    max-width: 620px;
    max-height: calc(100dvh - 24px);
    margin: 0 !important;
    display: flex;
    flex-direction: column;
    border-radius: 16px;
    touch-action: manipulation;
    overscroll-behavior: contain;
  }
  .el-dialog__header { flex: 0 0 auto; padding: 17px 48px 13px 17px; }
  .el-dialog__headerbtn { top: 8px; right: 8px; width: 42px; height: 42px; }
  .el-dialog__body { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 12px 17px 17px; }
  .el-dialog__footer { flex: 0 0 auto; padding: 12px 17px 16px; border-top: 1px solid #edf1ef; }
  .el-message-box { width: calc(100% - 28px) !important; }
  .el-card__header { padding: 15px 16px; }
  .el-card__body { padding: 16px; }
}
</style>
