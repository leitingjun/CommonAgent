<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="租户数" :value="stats.tenantCount" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="活跃会话" :value="stats.activeConversations" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="知识条目" :value="stats.totalKnowledge" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="API 状态" value="正常" />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="workflow-card" shadow="hover">
      <template #header>
        <span>Agent 工作流 (LangGraph)</span>
      </template>
      <pre class="workflow-graph">{{ workflowGraph }}</pre>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const stats = ref({ tenantCount: 0, activeConversations: 0, totalKnowledge: 0 })
const workflowGraph = ref('')

onMounted(async () => {
  try {
    const res = await axios.get('/api/admin/dashboard')
    stats.value = res.data
    workflowGraph.value = res.data.workflowGraph || ''
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  h2 { font-size: 22px; margin: 0 0 20px; color: #303133; }
}

.stats-row { margin-bottom: 24px; }

.stat-card {
  :deep(.el-statistic__head) { font-size: 14px; color: #909399; }
  :deep(.el-statistic__number) { font-size: 28px; font-weight: 700; }
}

.workflow-card {
  .workflow-graph {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.5;
    color: #303133;
    background: #fafafa;
    padding: 16px;
    border-radius: 8px;
    white-space: pre;
    overflow-x: auto;
  }
}
</style>
