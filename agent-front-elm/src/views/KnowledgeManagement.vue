<template>
  <div class="knowledge-management-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
          <el-button text @click="goToChat">情感陪伴</el-button>
          <el-button text @click="goToEmotionDiary">情绪日记</el-button>
          <el-button text @click="goToEmotionClassroom">情感课堂</el-button>
          <el-button text @click="goToKnowledgeManagement">知识库管理</el-button>
          <el-button text @click="goToFeedback">反馈与建议</el-button>
        </div>
      <div class="nav-right">
        <el-dropdown @command="handleCommand" v-if="user">
          <span class="el-dropdown-link">
            <el-avatar :size="36" :src="user.avatar" v-if="user.avatar"></el-avatar>
            <el-avatar :size="36" v-else>{{ user.username?.charAt(0) || 'U' }}</el-avatar>
            <span class="user-name">{{ user.username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="userManage" v-if="user?.isAdmin === 1">用户管理</el-dropdown-item>
              <el-dropdown-item command="knowledgeManage" v-if="user?.isAdmin === 1">知识库管理</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <div class="management-main">
      <div class="page-header">
        <h2>📚 知识库管理</h2>
        <p class="header-desc">管理知识库和文档，支持在线热更新</p>
      </div>

      <div class="knowledge-list-section">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索知识库名称"
              clearable
              style="width: 220px"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              <span style="margin-left: 6px;">搜索</span>
            </el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
          <div class="filter-right">
            <el-button type="primary" @click="showAddDialog">
              <el-icon><Plus /></el-icon>
              <span style="margin-left: 6px;">新建知识库</span>
            </el-button>
          </div>
        </div>

        <el-table
          :data="filteredKnowledgeList"
          v-loading="loading"
          stripe
          style="width: 100%"
          empty-text="暂无知识库"
          row-class-name="knowledge-row"
        >
          <el-table-column label="知识库信息" min-width="200">
            <template #default="{ row }">
              <div class="knowledge-info-cell">
                <div class="knowledge-icon">📚</div>
                <div class="knowledge-detail">
                  <div class="knowledge-name">{{ row.name }}</div>
                  <div class="knowledge-desc">{{ row.description || '暂无描述' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="表名" width="150">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.tableName }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="文档数量" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.documentCount || 0 }} 个</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '已启用' : '已停用' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="自动加载" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.autoLoad === 1 ? 'primary' : 'info'" size="small">
                {{ row.autoLoad === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="最后同步" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.lastSyncTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" size="medium" text @click="showDocuments(row)">
                  <el-icon><FolderOpened /></el-icon>
                  文档
                </el-button>
                <el-button type="warning" size="medium" text @click="handleHotReload(row)">
                  <el-icon><Refresh /></el-icon>
                  热更新
                </el-button>
                <el-button type="primary" size="medium" text @click="showEditDialog(row)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button type="danger" size="medium" text @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑知识库' : '新建知识库'"
      width="500px"
      class="knowledge-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="向量数据库表名" maxlength="30" :disabled="isEdit" />
          <template #extra>
            <div class="form-tip">
              表名规范：只能包含小写字母(a-z)、数字(0-9)和下划线(_)，且必须以字母开头，长度不超过30
            </div>
          </template>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="自动加载" prop="autoLoad">
          <el-switch v-model="form.autoLoad" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="documentDialogVisible"
      :title="'📄 ' + currentKnowledge?.name + ' - 文档管理'"
      width="1250px"
      class="document-dialog"
    >
      <div class="document-header">
        <div class="document-header-left">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="{ knowledgeBaseId: currentKnowledge?.id }"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            accept=".md"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              <span style="margin-left: 6px;">上传文档</span>
            </el-button>
          </el-upload>
          <span class="upload-tip">仅支持 .md 文件</span>
        </div>
        <div class="document-header-right" v-if="selectedDocuments.length > 0">
          <span class="selected-count">已选择 {{ selectedDocuments.length }} 项</span>
          <el-button type="success" size="medium" @click="handleBatchEnable">
            <el-icon><Check /></el-icon>
            批量启用
          </el-button>
          <el-button type="warning" size="medium" @click="handleBatchDisable">
            <el-icon><Close /></el-icon>
            批量停用
          </el-button>
          <el-button type="danger" size="medium" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>

      <el-table
        ref="documentTableRef"
        :data="documentList"
        v-loading="documentLoading"
        stripe
        style="width: 100%"
        empty-text="暂无文档"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name-cell">
              <el-icon class="file-icon"><Document /></el-icon>
              {{ row.fileName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="100" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '已启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="medium" text @click="handleDownloadDocument(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="medium"
              text
              @click="handleToggleDocStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="danger" size="medium" text @click="handleDeleteDocument(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowDown, Search, Plus, FolderOpened, Refresh, Edit, Delete, Upload, Document, Download, Check, Close
} from '@element-plus/icons-vue';
import {
  getKnowledgeBaseList,
  addKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  updateKnowledgeBaseStatus,
  updateKnowledgeBaseAutoLoad,
  hotReloadKnowledgeBase,
  getDocumentList,
  deleteDocument,
  updateDocumentStatus,
  batchUpdateDocumentStatus,
  batchDeleteDocuments,
  logout
} from '../api';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || null);
const knowledgeList = ref([]);
const loading = ref(false);
const searchKeyword = ref('');
const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);
const submitLoading = ref(false);
const form = ref({
  id: null,
  name: '',
  description: '',
  tableName: '',
  status: 1,
  autoLoad: 1
});
const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
  tableName: [
    { required: true, message: '请输入表名', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '表名格式不正确，只能包含小写字母、数字、下划线，且必须以字母开头',
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        if (value.length > 30) {
          callback(new Error('表名长度不能超过30个字符'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const documentDialogVisible = ref(false);
const documentList = ref([]);
const documentLoading = ref(false);
const currentKnowledge = ref(null);
const selectedDocuments = ref([]);
const documentTableRef = ref(null);

const API_BASE_URL = '/api';
const uploadUrl = computed(() => `${API_BASE_URL}/knowledgeBase/document/upload`);
const uploadHeaders = computed(() => {
  const userData = JSON.parse(localStorage.getItem('user') || '{}');
  return { Authorization: userData.token || '' };
});

const filteredKnowledgeList = computed(() => {
  if (!searchKeyword.value) return knowledgeList.value;
  const keyword = searchKeyword.value.toLowerCase();
  return knowledgeList.value.filter(kb =>
    kb.name?.toLowerCase().includes(keyword) ||
    kb.description?.toLowerCase().includes(keyword)
  );
});

/**
 * 加载知识库列表
 */
const loadKnowledgeList = async () => {
  loading.value = true;
  try {
    const res = await getKnowledgeBaseList();
    if (res.code === 0) {
      knowledgeList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载知识库列表失败:', error);
    ElMessage.error('加载知识库列表失败');
  } finally {
    loading.value = false;
  }
};

/**
 * 搜索
 */
const handleSearch = () => {
  // 使用 computed 自动过滤
};

/**
 * 重置过滤条件
 */
const resetFilters = () => {
  searchKeyword.value = '';
};

/**
 * 显示新建对话框
 */
const showAddDialog = () => {
  isEdit.value = false;
  form.value = {
    id: null,
    name: '',
    description: '',
    tableName: '',
    status: 1,
    autoLoad: 1
  };
  dialogVisible.value = true;
};

/**
 * 显示编辑对话框
 */
const showEditDialog = (row) => {
  isEdit.value = true;
  form.value = {
    id: row.id,
    name: row.name,
    description: row.description,
    tableName: row.tableName,
    status: row.status,
    autoLoad: row.autoLoad
  };
  dialogVisible.value = true;
};

/**
 * 提交表单
 */
const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      let res;
      if (isEdit.value) {
        res = await updateKnowledgeBase(form.value);
      } else {
        res = await addKnowledgeBase(form.value);
      }
      if (res.code === 0) {
        ElMessage.success(isEdit.value ? '编辑成功' : '创建成功');
        dialogVisible.value = false;
        loadKnowledgeList();
      } else {
        ElMessage.error(res.message || '操作失败');
      }
    } catch (error) {
      console.error('提交失败:', error);
      ElMessage.error('操作失败');
    } finally {
      submitLoading.value = false;
    }
  });
};

/**
 * 删除知识库
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除知识库「${row.name}」吗？这将同时删除所有关联文档。`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    const res = await deleteKnowledgeBase(row.id);
    if (res.code === 0) {
      ElMessage.success('删除成功');
      loadKnowledgeList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

/**
 * 热更新知识库
 */
const handleHotReload = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要热更新知识库「${row.name}」吗？这将重新加载所有文档到向量数据库。`,
      '确认热更新',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    const res = await hotReloadKnowledgeBase(row.id);
    if (res.code === 0) {
      ElMessage.success('热更新成功');
      loadKnowledgeList();
    } else {
      ElMessage.error(res.message || '热更新失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('热更新失败:', error);
      ElMessage.error('热更新失败');
    }
  }
};

/**
 * 显示文档列表
 */
const showDocuments = async (knowledge) => {
  currentKnowledge.value = knowledge;
  documentDialogVisible.value = true;
  documentLoading.value = true;
  documentList.value = [];
  try {
    const res = await getDocumentList(knowledge.id);
    if (res.code === 0) {
      documentList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载文档列表失败:', error);
    ElMessage.error('加载文档列表失败');
  } finally {
    documentLoading.value = false;
  }
};

/**
 * 上传前检查
 */
const beforeUpload = (file) => {
  const isMd = file.name.endsWith('.md');
  if (!isMd) {
    ElMessage.error('只能上传 .md 文件');
    return false;
  }
  return true;
};

/**
 * 上传成功
 */
const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    ElMessage.success('上传成功');
    showDocuments(currentKnowledge.value);
    loadKnowledgeList();
  } else {
    ElMessage.error(response.message || '上传失败');
  }
};

/**
 * 上传失败
 */
const handleUploadError = () => {
  ElMessage.error('上传失败');
};

/**
 * 切换文档状态
 */
const handleToggleDocStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1;
  try {
    const res = await updateDocumentStatus(row.id, newStatus);
    if (res.code === 0) {
      ElMessage.success(newStatus === 1 ? '已启用' : '已停用');
      showDocuments(currentKnowledge.value);
      loadKnowledgeList();
    }
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败');
  }
};

/**
 * 删除文档
 */
const handleDeleteDocument = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${row.fileName}」吗？`,
      '确认删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    const res = await deleteDocument(row.id);
    if (res.code === 0) {
      ElMessage.success('删除成功');
      showDocuments(currentKnowledge.value);
      loadKnowledgeList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

/**
 * 下载文档
 */
const handleDownloadDocument = (row) => {
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  const link = document.createElement('a');
  link.href = `/api/knowledgeBase/document/download?id=${row.id}`;
  link.download = row.fileName || 'document.md';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/**
 * 表格选择变化
 */
const handleSelectionChange = (selection) => {
  selectedDocuments.value = selection;
};

/**
 * 批量启用
 */
const handleBatchEnable = async () => {
  if (selectedDocuments.value.length === 0) return;
  try {
    const ids = selectedDocuments.value.map(doc => doc.id);
    const res = await batchUpdateDocumentStatus(ids, 1);
    if (res.code === 0) {
      ElMessage.success('批量启用成功');
      selectedDocuments.value = [];
      showDocuments(currentKnowledge.value);
      loadKnowledgeList();
    }
  } catch (error) {
    console.error('批量启用失败:', error);
    ElMessage.error('批量启用失败');
  }
};

/**
 * 批量停用
 */
const handleBatchDisable = async () => {
  if (selectedDocuments.value.length === 0) return;
  try {
    const ids = selectedDocuments.value.map(doc => doc.id);
    const res = await batchUpdateDocumentStatus(ids, 0);
    if (res.code === 0) {
      ElMessage.success('批量停用成功');
      selectedDocuments.value = [];
      showDocuments(currentKnowledge.value);
      loadKnowledgeList();
    }
  } catch (error) {
    console.error('批量停用失败:', error);
    ElMessage.error('批量停用失败');
  }
};

/**
 * 批量删除
 */
const handleBatchDelete = async () => {
  if (selectedDocuments.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedDocuments.value.length} 个文档吗？`,
      '确认批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    const ids = selectedDocuments.value.map(doc => doc.id);
    const res = await batchDeleteDocuments(ids);
    if (res.code === 0) {
      ElMessage.success('批量删除成功');
      selectedDocuments.value = [];
      showDocuments(currentKnowledge.value);
      loadKnowledgeList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error);
      ElMessage.error('批量删除失败');
    }
  }
};

/**
 * 格式化日期时间
 */
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
};

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

/**
 * 下拉菜单命令处理
 */
const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile');
  } else if (command === 'userManage') {
    router.push('/user-management');
  } else if (command === 'knowledgeManage') {
    router.push('/knowledge-management');
  } else if (command === 'logout') {
    handleLogout();
  }
};

/**
 * 退出登录
 */
const handleLogout = async () => {
  try {
    await logout();
  } catch (error) {
    console.error('登出失败:', error);
  }
  localStorage.removeItem('user');
  router.push('/');
};

const goToHome = () => router.push('/');
const goToChat = () => router.push('/chat');
const goToEmotionDiary = () => router.push('/emotion-diary');
const goToEmotionClassroom = () => router.push('/emotion-classroom');

const goToKnowledgeManagement = () => {
  router.push('/knowledge-management');
};

const goToFeedback = () => {
  if (user.value?.isAdmin === 1) {
    router.push('/feedback-management');
  } else {
    router.push('/feedback');
  }
};

onMounted(() => {
  loadKnowledgeList();
});
</script>

<style scoped>
.knowledge-management-container {
  min-height: 100vh;
  background-image: url('../image/bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-logo {
  font-size: 32px;
}

.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.nav-center {
  display: flex;
  gap: 16px;
}

.nav-center .el-button {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
}

.nav-center .el-button:hover {
  color: #409eff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 15px;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.management-main {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 6px;
}

.header-desc {
  color: #909399;
  font-size: 14px;
}

.knowledge-list-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.knowledge-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.knowledge-icon {
  font-size: 32px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f7ff;
  border-radius: 8px;
}

.knowledge-detail {
  display: flex;
  flex-direction: column;
}

.knowledge-name {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}

.knowledge-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.knowledge-row:hover > td) {
  background-color: #f0f7ff !important;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.document-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.document-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-count {
  font-size: 14px;
  color: #409eff;
  font-weight: 500;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #409eff;
  font-size: 18px;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 8px;
  justify-items: center;
}

.action-buttons .el-button {
  margin: 0;
}
</style>
