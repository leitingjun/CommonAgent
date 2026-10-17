<template>
  <div class="tenant-management">
    <div class="page-header">
      <h2>租户管理</h2>
      <el-button type="primary" @click="showDialog = true">注册新租户</el-button>
    </div>

    <!-- 租户列表 -->
    <el-table :data="tenants" stripe style="width: 100%">
      <el-table-column prop="tenantId" label="租户ID" width="120" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="industry" label="行业" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ industryLabel(row.industry) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '活跃' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contactEmail" label="联系邮箱" width="200" />
      <el-table-column prop="maxConcurrentSessions" label="最大会话" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="editTenant(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            size="small"
            type="danger"
            @click="handleDeactivate(row.tenantId)"
          >
            停用
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 注册/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="editingTenant ? '编辑租户' : '注册新租户'"
      width="560px"
    >
      <el-form :model="form" label-width="120px">
        <el-form-item label="租户ID" required>
          <el-input v-model="form.tenantId" :disabled="!!editingTenant" placeholder="唯一标识，如: shop001" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="公司/品牌名称" />
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="form.industry" style="width:100%">
            <el-option label="通用" value="general" />
            <el-option label="电商" value="ecommerce" />
            <el-option label="金融" value="finance" />
            <el-option label="医疗" value="healthcare" />
            <el-option label="教育" value="education" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" />
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input v-model="form.welcomeMessage" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="自定义Prompt">
          <el-input v-model="form.promptTemplate" type="textarea" :rows="4" placeholder="留空则使用行业默认模板" />
        </el-form-item>
        <el-form-item label="最大会话数">
          <el-input-number v-model="form.maxConcurrentSessions" :min="1" :max="10000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTenant ? '保存' : '注册' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { tenantApi, type TenantInfo } from '@/api/tenant'
import { ElMessage, ElMessageBox } from 'element-plus'

const tenants = ref<TenantInfo[]>([])
const showDialog = ref(false)
const editingTenant = ref<TenantInfo | null>(null)
const submitting = ref(false)

const form = reactive<TenantInfo>({
  tenantId: '',
  name: '',
  industry: 'general',
  contactEmail: '',
  welcomeMessage: '',
  promptTemplate: '',
  maxConcurrentSessions: 100,
})

const industryLabels: Record<string, string> = {
  general: '通用', ecommerce: '电商', finance: '金融',
  healthcare: '医疗', education: '教育',
}

function industryLabel(v: string) { return industryLabels[v] || v }

async function loadTenants() {
  try {
    tenants.value = await tenantApi.list()
  } catch (e) {
    ElMessage.error('加载租户列表失败')
  }
}

function editTenant(tenant: TenantInfo) {
  editingTenant.value = tenant
  Object.assign(form, tenant)
  showDialog.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (editingTenant.value) {
      await tenantApi.update(form.tenantId, form)
      ElMessage.success('更新成功')
    } else {
      await tenantApi.register(form)
      ElMessage.success('注册成功')
    }
    showDialog.value = false
    editingTenant.value = null
    resetForm()
    await loadTenants()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDeactivate(tenantId: string) {
  try {
    await ElMessageBox.confirm('确定要停用该租户吗？', '确认', { type: 'warning' })
    await tenantApi.deactivate(tenantId)
    ElMessage.success('已停用')
    await loadTenants()
  } catch { /* 取消 */ }
}

function resetForm() {
  form.tenantId = ''
  form.name = ''
  form.industry = 'general'
  form.contactEmail = ''
  form.welcomeMessage = ''
  form.promptTemplate = ''
  form.maxConcurrentSessions = 100
}

onMounted(loadTenants)
</script>

<style scoped lang="scss">
.tenant-management {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    h2 { margin: 0; font-size: 22px; }
  }
}
</style>
