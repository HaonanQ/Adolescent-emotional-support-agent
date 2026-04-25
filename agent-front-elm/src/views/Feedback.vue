<template>
  <div class="feedback-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>

    <div class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="goBack" style="cursor: pointer;">
        <el-icon><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <h1>反馈与建议</h1>
      </div>
      <div class="nav-right">
        <el-button type="primary" class="submit-btn" @click="showAddDialog = true">提交反馈</el-button>
      </div>
    </div>

    <div class="feedback-main" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
      <div v-if="feedbackList.length === 0" class="empty-state">
        <el-empty description="暂无反馈记录" :image-size="120" />
      </div>
      
      <div v-else class="feedback-list">
        <div v-for="(item, index) in feedbackList" :key="item.id" class="feedback-item" :style="{ animationDelay: `${index * 0.1}s` }">
          <div class="feedback-header">
            <div class="feedback-title">{{ item.title }}</div>
            <el-tag :type="getStatusType(item.status)" size="small" class="status-tag">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          <div class="feedback-content">{{ item.content }}</div>
          <div class="feedback-footer">
            <div class="feedback-info">
              <el-tag :type="getTypeColor(item.type)" size="small" effect="plain" class="type-tag">
                {{ getTypeText(item.type) }}
              </el-tag>
              <span class="feedback-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div v-if="item.adminReply" class="admin-reply">
              <div class="reply-label">管理员回复：</div>
              <div class="reply-content">{{ item.adminReply }}</div>
            </div>
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

    <!-- 添加反馈对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="提交反馈"
      width="600px"
      :close-on-click-modal="false"
      class="feedback-dialog"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="formData.title"
            placeholder="请输入反馈标题"
            maxlength="100"
            show-word-limit
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type" class="radio-group">
            <el-radio label="suggestion" class="radio-item">建议</el-radio>
            <el-radio label="problem" class="radio-item">问题</el-radio>
            <el-radio label="other" class="radio-item">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            placeholder="请详细描述您的反馈内容..."
            maxlength="500"
            show-word-limit
            class="form-textarea"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button class="cancel-btn" @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" class="submit-btn" @click="submitFeedback" :loading="submitting">
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
import { addFeedback, getUserFeedbackList } from '../api';

const router = useRouter();
const feedbackList = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const showAddDialog = ref(false);
const submitting = ref(false);
const formRef = ref(null);

const formData = ref({
  title: '',
  type: 'suggestion',
  content: ''
});

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
};

const goBack = () => {
  router.go(-1);
};

const loadFeedbackList = async () => {
  try {
    const response = await getUserFeedbackList({
      current: currentPage.value,
      pageSize: pageSize.value
    });
    if (response.code === 0 && response.data) {
      feedbackList.value = response.data.records || [];
      total.value = response.data.total || 0;
    }
  } catch (error) {
    console.error('加载反馈列表失败:', error);
    ElMessage.error('加载反馈列表失败');
  }
};

const submitFeedback = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        const response = await addFeedback(formData.value);
        if (response.code === 0) {
          ElMessage.success('提交成功，感谢您的反馈！');
          showAddDialog.value = false;
          formData.value = { title: '', type: 'suggestion', content: '' };
          loadFeedbackList();
        } else {
          ElMessage.error(response.message || '提交失败');
        }
      } catch (error) {
        console.error('提交反馈失败:', error);
        ElMessage.error('提交失败，请稍后重试');
      } finally {
        submitting.value = false;
      }
    }
  });
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
.feedback-container {
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

.submit-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  padding: 8px 16px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.feedback-main {
  max-width: 900px;
  margin: 32px auto;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
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
  margin-bottom: 12px;
}

.feedback-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  flex: 1;
  margin-right: 12px;
}

.status-tag {
  border-radius: var(--radius-full);
  padding: 2px 10px;
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

.feedback-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feedback-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
.feedback-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
}

.feedback-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.feedback-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.feedback-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.form-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 10px 16px;
  transition: all var(--transition-base);
}

.form-input :deep(.el-input__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
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
  .feedback-main {
    max-width: 800px;
    padding: 20px;
  }
}

@media (max-width: 992px) {
  .feedback-main {
    max-width: 100%;
    margin: 24px;
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }

  .nav-center h1 {
    font-size: 16px;
  }

  .feedback-main {
    margin: 16px;
    padding: 16px;
  }

  .feedback-item {
    padding: 16px;
  }

  .radio-group {
    flex-direction: column;
    gap: 12px;
  }

  .feedback-dialog :deep(.el-dialog) {
    width: 95% !important;
  }

  .feedback-dialog :deep(.el-dialog__body) {
    padding: 16px;
  }
}

/* 滚动条样式 */
.feedback-main::-webkit-scrollbar {
  width: 6px;
}

.feedback-main::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.feedback-main::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.feedback-main::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
