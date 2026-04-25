<template>
  <div class="feedback-management-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>

    <div class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="goBack" style="cursor: pointer;">
        <el-icon><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <h1>反馈管理</h1>
      </div>
      <div class="nav-right"></div>
    </div>

    <div class="management-main" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
      <div class="page-header">
        <h2>用户反馈列表</h2>
        <div class="filter-section">
          <el-select v-model="filterStatus" placeholder="处理状态" clearable @change="loadFeedbackList" style="width: 120px; margin-right: 12px;" class="filter-select">
            <el-option label="待处理" value="pending" />
            <el-option label="已处理" value="processed" />
            <el-option label="已忽略" value="ignored" />
          </el-select>
          <el-select v-model="filterType" placeholder="反馈类型" clearable @change="loadFeedbackList" style="width: 120px;" class="filter-select">
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
        <div v-for="(item, index) in feedbackList" :key="item.id" class="feedback-item" :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="feedback-header">
            <div class="user-info">
              <span class="user-nickname">{{ item.userNickname }}</span>
              <span class="user-id">ID: {{ item.userId }}</span>
            </div>
            <el-tag :type="getStatusType(item.status)" size="small" class="status-tag">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          
          <div class="feedback-title">{{ item.title }}</div>
          <div class="feedback-content">{{ item.content }}</div>
          
          <div class="feedback-meta">
            <el-tag :type="getTypeColor(item.type)" size="small" effect="plain" class="type-tag">
              {{ getTypeText(item.type) }}
            </el-tag>
            <span class="feedback-time">{{ formatTime(item.createTime) }}</span>
          </div>

          <div v-if="item.adminReply" class="admin-reply">
            <div class="reply-label">管理员回复：</div>
            <div class="reply-content">{{ item.adminReply }}</div>
          </div>

          <div class="action-buttons">
            <el-button type="primary" size="small" class="primary-btn" @click="openReplyDialog(item)">
              {{ item.adminReply ? '修改回复' : '回复' }}
            </el-button>
            <el-button v-if="item.status === 'pending'" type="success" size="small" class="success-btn" @click="updateStatus(item.id, 'processed')">
              标记已处理
            </el-button>
            <el-button v-if="item.status === 'pending'" type="warning" size="small" class="warning-btn" @click="updateStatus(item.id, 'ignored')">
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
          class="pagination"
        />
      </div>
    </div>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="showReplyDialog"
      title="回复反馈"
      width="600px"
      :close-on-click-modal="false"
      class="reply-dialog"
    >
      <el-form :model="replyForm" ref="replyFormRef" label-width="80px">
        <el-form-item label="反馈标题">
          <div class="form-static">{{ currentFeedback?.title }}</div>
        </el-form-item>
        <el-form-item label="反馈内容">
          <div class="form-static">{{ currentFeedback?.content }}</div>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-radio-group v-model="replyForm.status" class="radio-group">
            <el-radio label="processed" class="radio-item">已处理</el-radio>
            <el-radio label="ignored" class="radio-item">已忽略</el-radio>
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
            class="form-textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button class="cancel-btn" @click="showReplyDialog = false">取消</el-button>
          <el-button type="primary" class="submit-btn" @click="submitReply" :loading="submitting">
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
  background: var(--color-background);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 导航栏 */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  box-shadow: var(--shadow-soft);
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
  color: var(--color-text-secondary);
  font-size: 15px;
  padding: 8px 16px;
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
}

.nav-left:hover {
  color: var(--color-text-primary);
  background: var(--color-surface-soft);
}

.nav-center h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.management-main {
  max-width: 1200px;
  margin: 32px auto;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  flex-wrap: wrap;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--color-text-primary);
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-select :deep(.el-select__wrapper) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  transition: all var(--transition-base);
}

.filter-select :deep(.el-select__wrapper:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feedback-item {
  padding: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 107, 157, 0.1);
  transition: all var(--transition-base);
  animation: fadeInUp 0.4s ease-out both;
  box-shadow: var(--shadow-soft);
}

.feedback-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.user-nickname {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.user-id {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.status-tag {
  border-radius: var(--radius-full);
  padding: 2px 10px;
}

.feedback-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.feedback-content {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(255, 107, 157, 0.05);
  border-radius: var(--radius-lg);
}

.feedback-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.type-tag {
  border-radius: var(--radius-full);
  padding: 2px 10px;
  border: 1px solid var(--color-primary-light);
}

.feedback-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.admin-reply {
  padding: 16px;
  background: rgba(64, 158, 255, 0.05);
  border-radius: var(--radius-lg);
  border-left: 3px solid var(--color-primary);
  margin-bottom: 16px;
  animation: fadeInLeft 0.4s ease-out;
}

.reply-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: 6px;
}

.reply-content {
  font-size: 14px;
  color: var(--color-text-primary);
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
  flex-wrap: wrap;
}

.primary-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  padding: 6px 12px;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.success-btn {
  background: var(--color-success);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  padding: 6px 12px;
}

.success-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.warning-btn {
  background: var(--color-warning);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  padding: 6px 12px;
}

.warning-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
}

.pagination :deep(.el-pagination__button) {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 107, 157, 0.15);
  transition: all var(--transition-base);
}

.pagination :deep(.el-pagination__button:hover) {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
}

.pagination :deep(.el-pagination__button.is-current) {
  background: var(--gradient-1);
  border-color: var(--color-primary);
  color: white;
}

/* 对话框样式 */
.reply-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
}

.reply-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.reply-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.reply-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.form-static {
  padding: 10px 12px;
  background: rgba(255, 107, 157, 0.05);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 107, 157, 0.1);
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.form-textarea :deep(.el-textarea__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 12px 16px;
  transition: all var(--transition-base);
  resize: vertical;
}

.form-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.radio-group {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.radio-item :deep(.el-radio__label) {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.radio-item :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: var(--gradient-1);
  border-color: var(--color-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
  background: rgba(255, 255, 255, 0.8);
}

.cancel-btn {
  background: transparent;
  border: 2px solid var(--color-primary-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
}

.cancel-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.submit-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

/* 装饰背景元素 */
.decorative-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, var(--color-primary-light) 0%, transparent 70%);
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}

.circle-1 {
  width: 600px;
  height: 600px;
  top: -150px;
  right: -150px;
  opacity: 0.3;
  animation: float 6s ease-in-out infinite;
}

.circle-2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -100px;
  opacity: 0.3;
  animation: float 8s ease-in-out infinite reverse;
}

/* 动画 */
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .management-main {
    max-width: 1000px;
    padding: 20px;
  }
}

@media (max-width: 992px) {
  .management-main {
    max-width: 100%;
    margin: 24px;
    padding: 20px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }

  .nav-center h1 {
    font-size: 16px;
  }

  .management-main {
    margin: 16px;
    padding: 16px;
  }

  .feedback-item {
    padding: 16px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .radio-group {
    flex-direction: column;
    gap: 12px;
  }

  .reply-dialog :deep(.el-dialog) {
    width: 95% !important;
  }

  .reply-dialog :deep(.el-dialog__body) {
    padding: 16px;
  }
}

/* 滚动条样式 */
.management-main::-webkit-scrollbar {
  width: 6px;
}

.management-main::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.management-main::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.management-main::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
