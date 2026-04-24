<template>
  <div class="feedback-management-container">
    <div class="navbar">
      <div class="nav-left" @click="goBack">
        <el-icon><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <h1>反馈管理</h1>
      </div>
      <div class="nav-right"></div>
    </div>

    <div class="management-main">
      <div class="page-header">
        <h2>用户反馈列表</h2>
        <div class="filter-section">
          <el-select v-model="filterStatus" placeholder="处理状态" clearable @change="loadFeedbackList" style="width: 120px; margin-right: 12px;">
            <el-option label="待处理" value="pending" />
            <el-option label="已处理" value="processed" />
            <el-option label="已忽略" value="ignored" />
          </el-select>
          <el-select v-model="filterType" placeholder="反馈类型" clearable @change="loadFeedbackList" style="width: 120px;">
            <el-option label="建议" value="suggestion" />
            <el-option label="问题" value="problem" />
            <el-option label="其他" value="other" />
          </el-select>
        </div>
      </div>

      <div v-if="feedbackList.length === 0" class="empty-state">
        <el-empty description="暂无反馈记录" :image-size="120" />
      </div>

      <div v-else class="feedback-list">
        <div v-for="item in feedbackList" :key="item.id" class="feedback-item">
          <div class="feedback-header">
            <div class="user-info">
              <span class="user-nickname">{{ item.userNickname }}</span>
              <span class="user-id">ID: {{ item.userId }}</span>
            </div>
            <el-tag :type="getStatusType(item.status)" size="small">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          
          <div class="feedback-title">{{ item.title }}</div>
          <div class="feedback-content">{{ item.content }}</div>
          
          <div class="feedback-meta">
            <el-tag :type="getTypeColor(item.type)" size="small" effect="plain">
              {{ getTypeText(item.type) }}
            </el-tag>
            <span class="feedback-time">{{ formatTime(item.createTime) }}</span>
          </div>

          <div v-if="item.adminReply" class="admin-reply">
            <div class="reply-label">管理员回复：</div>
            <div class="reply-content">{{ item.adminReply }}</div>
          </div>

          <div class="action-buttons">
            <el-button type="primary" size="small" @click="openReplyDialog(item)">
              {{ item.adminReply ? '修改回复' : '回复' }}
            </el-button>
            <el-button v-if="item.status === 'pending'" type="success" size="small" @click="updateStatus(item.id, 'processed')">
              标记已处理
            </el-button>
            <el-button v-if="item.status === 'pending'" type="warning" size="small" @click="updateStatus(item.id, 'ignored')">
              忽略
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadFeedbackList"
        />
      </div>
    </div>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="showReplyDialog"
      title="回复反馈"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="replyForm" ref="replyFormRef" label-width="80px">
        <el-form-item label="反馈标题">
          <div>{{ currentFeedback?.title }}</div>
        </el-form-item>
        <el-form-item label="反馈内容">
          <div>{{ currentFeedback?.content }}</div>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-radio-group v-model="replyForm.status">
            <el-radio label="processed">已处理</el-radio>
            <el-radio label="ignored">已忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="replyForm.adminReply"
            type="textarea"
            :rows="6"
            placeholder="请输入回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showReplyDialog = false">取消</el-button>
          <el-button type="primary" @click="submitReply" :loading="submitting">
            提交
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { getAdminFeedbackList, replyFeedback } from '../api';

const router = useRouter();
const feedbackList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filterStatus = ref('');
const filterType = ref('');
const showReplyDialog = ref(false);
const submitting = ref(false);
const replyFormRef = ref(null);
const currentFeedback = ref(null);

const replyForm = ref({
  id: null,
  adminReply: '',
  status: 'processed'
});

const goBack = () => {
  router.go(-1);
};

const loadFeedbackList = async () => {
  try {
    const params = {
      current: currentPage.value,
      pageSize: pageSize.value
    };
    if (filterStatus.value) params.status = filterStatus.value;
    if (filterType.value) params.type = filterType.value;

    const response = await getAdminFeedbackList(params);
    if (response.code === 0 && response.data) {
      feedbackList.value = response.data.records || [];
      total.value = response.data.total || 0;
    }
  } catch (error) {
    console.error('加载反馈列表失败:', error);
    ElMessage.error('加载反馈列表失败');
  }
};

const openReplyDialog = (feedback) => {
  currentFeedback.value = feedback;
  replyForm.value = {
    id: feedback.id,
    adminReply: feedback.adminReply || '',
    status: feedback.status === 'pending' ? 'processed' : feedback.status
  };
  showReplyDialog.value = true;
};

const submitReply = async () => {
  submitting.value = true;
  try {
    const response = await replyFeedback(replyForm.value);
    if (response.code === 0) {
      ElMessage.success('回复成功');
      showReplyDialog.value = false;
      loadFeedbackList();
    } else {
      ElMessage.error(response.message || '回复失败');
    }
  } catch (error) {
    console.error('回复失败:', error);
    ElMessage.error('回复失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
};

const updateStatus = async (id, status) => {
  try {
    const response = await replyFeedback({ id, status, adminReply: '' });
    if (response.code === 0) {
      ElMessage.success('状态更新成功');
      loadFeedbackList();
    } else {
      ElMessage.error(response.message || '更新失败');
    }
  } catch (error) {
    console.error('更新状态失败:', error);
    ElMessage.error('更新失败，请稍后重试');
  }
};

const getStatusType = (status) => {
  const types = {
    pending: 'warning',
    processed: 'success',
    ignored: 'info'
  };
  return types[status] || 'info';
};

const getStatusText = (status) => {
  const texts = {
    pending: '待处理',
    processed: '已处理',
    ignored: '已忽略'
  };
  return texts[status] || status;
};

const getTypeColor = (type) => {
  const colors = {
    suggestion: 'primary',
    problem: 'danger',
    other: 'info'
  };
  return colors[type] || 'info';
};

const getTypeText = (type) => {
  const texts = {
    suggestion: '建议',
    problem: '问题',
    other: '其他'
  };
  return texts[type] || type;
};

const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(() => {
  loadFeedbackList();
});
</script>

<style scoped>
.feedback-management-container {
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
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 56px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #64748b;
  font-size: 15px;
}

.nav-left:hover {
  color: #334155;
}

.nav-center h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.management-main {
  max-width: 1200px;
  margin: 32px auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #334155;
}

.filter-section {
  display: flex;
  align-items: center;
}

.empty-state {
  padding: 60px 0;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-item {
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.feedback-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-nickname {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.user-id {
  font-size: 12px;
  color: #94a3b8;
}

.feedback-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.feedback-content {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 12px;
}

.feedback-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.feedback-time {
  font-size: 12px;
  color: #94a3b8;
}

.admin-reply {
  padding: 12px;
  background: #ecf5ff;
  border-radius: 8px;
  border-left: 3px solid #409eff;
  margin-bottom: 12px;
}

.reply-label {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 6px;
}

.reply-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
