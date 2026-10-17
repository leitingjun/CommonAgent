<template>
  <div class="admin-container">
    <aside class="admin-sidebar">
      <h3 @click="$router.push('/')" style="cursor: pointer;">🤖 CommonAgent</h3>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/admin">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/tenants">
          <el-icon><OfficeBuilding /></el-icon>
          <span>租户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Document /></el-icon>
          <span>知识库管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-back">
        <el-button text @click="$router.push('/chat')">
          ← 返回聊天
        </el-button>
      </div>
    </aside>
    <main class="admin-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.admin-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

.admin-sidebar {
  width: 220px;
  background: #304156;
  color: #fff;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  h3 {
    padding: 20px;
    margin: 0;
    font-size: 18px;
    text-align: center;
    border-bottom: 1px solid rgba(255,255,255,0.1);
  }
  :deep(.el-menu) {
    border-right: none;
    background: transparent;
    .el-menu-item {
      color: #bfcbd9;
      &:hover, &.is-active { color: #fff; background: rgba(255,255,255,0.08); }
    }
  }
  .sidebar-back {
    margin-top: auto;
    padding: 16px;
    :deep(.el-button) { color: #bfcbd9; }
  }
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
