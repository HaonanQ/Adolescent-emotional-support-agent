<template>
  <div class="feedback-container">
    <div class="navbar">
      <div class="nav-left" @click="goBack">
        <el-icon><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <h1>反馈与建议</h1>
      </div>
      <div class="nav-right">
        <el-button type="primary" @click="showAddDialog = true">提交反馈</el-button>
      </div>
    </div>

    <div class="feedback-main">
      <div v-if="feedbackList.length === 0" class="empty-state">
        <el-empty description="暂无反馈记录" :image-size="120" />
      </div>
      
      <div v-else class="feedback-list">
        <div v-for="item in feedbackList" :key="item.id" class="feedback-item">
          <div class="feedback-header">
            <div class="feedback-title">{{ item.title }}</div>
            <el-tag :type="getStatusType(item.status)" size="small">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
          <div class="feedback-content">{{ item.content }}</div>
          <div class="feedback-footer">
            <div class="feedback-info">
              <el-tag :type="getTypeColor(item.type)" size="small" effect="plain">
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
        />
      </div>
    </div>

    <!-- 添加反馈对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="提交反馈"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="formData.title"
            placeholder="请输入反馈标题（不超过10个字）"
            maxlength="10"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio label="suggestion">建议</el-radio>
            <el-radio label="problem">问题</el-radio>
            <el-radio label="other">其他</el-radio>
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
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="submitFeedback" :loading="submitting">
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
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 10, message: '标题长度不能超过10个字符', trigger: 'blur' }
  ],
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

.feedback-main {
  max-width: 900px;
  margin: 20px auto;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.empty-state {
  padding: 60px 0;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.feedback-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
}

.feedback-content {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 12px;
}

.feedback-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-info {
  display: flex;
  align-items: center;
  gap: 12px;
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
