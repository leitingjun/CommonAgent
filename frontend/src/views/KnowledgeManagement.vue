<template>
  <div class="knowledge-management">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-select v-model="selectedTenant" placeholder="选择租户" size="default" style="width: 160px">
          <el-option label="演示公司" value="demo" />
          <el-option label="示例电商" value="shop123" />
          <el-option label="示例金融" value="bank456" />
        </el-select>
        <el-button type="primary" @click="showAddDialog = true">添加知识</el-button>
        <el-button @click="handleEnsureIndex">初始化索引</el-button>
      </div>
    </div>

    <!-- 知识列表 -->
    <el-table :data="knowledgeList" stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="referenceCount" label="引用" width="80" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.knowledgeId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加知识对话框 -->
    <el-dialog v-model="showAddDialog" title="添加知识条目" width="600px">
      <el-form :model="knowledgeForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="knowledgeForm.title" placeholder="知识标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="knowledgeForm.content"
            type="textarea"
            :rows="8"
            placeholder="知识内容，将用于向量检索匹配"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="knowledgeForm.category" placeholder="如: 退换货政策、常见问题" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="knowledgeForm.tags" placeholder="逗号分隔，如: 退款,售后,政策" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAddKnowledge">添加</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量导入知识" width="600px">
      <el-alert
        title="支持 CSV/JSON 格式批量导入。CSV 需包含 title,content,category,tags 列。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-input
        v-model="batchJson"
        type="textarea"
        :rows="12"
        placeholder='[{"title":"标题","content":"内容","category":"分类","tags":"标签1,标签2"}]'
      />
      <template #footer>
        <el-button @click="showBatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleBatchImport">
          导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { knowledgeApi, type KnowledgeItem } from '@/api/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'

const selectedTenant = ref('demo')
const knowledgeList = ref<KnowledgeItem[]>([])
const loading = ref(false)
const adding = ref(false)
const importing = ref(false)
const showAddDialog = ref(false)
const showBatchDialog = ref(false)
const batchJson = ref('')

const knowledgeForm = reactive<KnowledgeItem>({
  title: '',
  content: '',
  category: '',
  tags: '',
})

async function loadKnowledge() {
  loading.value = true
  try {
    knowledgeList.value = await knowledgeApi.list(selectedTenant.value)
  } catch (e) {
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

async function handleAddKnowledge() {
  if (!knowledgeForm.title || !knowledgeForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  adding.value = true
  try {
    await knowledgeApi.add(selectedTenant.value, { ...knowledgeForm })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    resetForm()
    await loadKnowledge()
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    adding.value = false
  }
}

async function handleDelete(knowledgeId: string) {
  try {
    await ElMessageBox.confirm('确定要删除该知识条目吗？', '确认', { type: 'warning' })
    await knowledgeApi.delete(selectedTenant.value, knowledgeId)
    ElMessage.success('删除成功')
    await loadKnowledge()
  } catch { /* 取消 */ }
}

async function handleEnsureIndex() {
  try {
    await knowledgeApi.ensureIndex(selectedTenant.value)
    ElMessage.success('索引已就绪')
  } catch (e) {
    ElMessage.error('索引初始化失败')
  }
}

async function handleBatchImport() {
  if (!batchJson.value.trim()) return
  importing.value = true
  try {
    const items = JSON.parse(batchJson.value)
    const result = await knowledgeApi.batchImport(selectedTenant.value, items)
    ElMessage.success(`成功导入 ${result.imported}/${result.total} 条`)
    showBatchDialog.value = false
    batchJson.value = ''
    await loadKnowledge()
  } catch (e) {
    ElMessage.error('导入失败，请检查 JSON 格式')
  } finally {
    importing.value = false
  }
}

function resetForm() {
  knowledgeForm.title = ''
  knowledgeForm.content = ''
  knowledgeForm.category = ''
  knowledgeForm.tags = ''
}

watch(selectedTenant, loadKnowledge)
onMounted(loadKnowledge)
</script>

<style scoped lang="scss">
.knowledge-management {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h2 { margin: 0; font-size: 22px; }
    .header-actions {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }
}
</style>
