<template>
  <div class="emotion-diary-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>
    
    <nav class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button class="nav-btn" @click="goToChat">情感陪伴</el-button>
        <el-button class="nav-btn" @click="goToEmotionDiary">情绪日记</el-button>
        <el-button class="nav-btn" @click="goToEmotionClassroom">情感课堂</el-button>
        <el-button class="nav-btn" @click="goToKnowledgeManagement" v-if="user?.isAdmin === 1">知识库管理</el-button>
        <el-button class="nav-btn" @click="goToFeedback">反馈与建议</el-button>
      </div>
      <div class="nav-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="36" :src="user.avatar" v-if="user.avatar" class="user-avatar"></el-avatar>
            <el-avatar :size="36" v-else class="user-avatar">{{ user.username?.charAt(0) || 'U' }}</el-avatar>
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

    <div class="diary-main">
      <aside class="timeline-sidebar" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
        <div class="timeline-header">
          <h3>我的日记</h3>
          <el-button type="primary" circle @click="resetForm" title="新建日记" class="new-diary-btn">
            <el-icon><plus /></el-icon>
          </el-button>
        </div>
        <div class="timeline-list">
          <el-empty v-if="diaryList.length === 0" description="还没有日记" :image-size="60">
            <template #image>
              <span class="empty-diary-icon">📔</span>
            </template>
          </el-empty>
          <div
            v-for="diary in diaryList"
            :key="diary.id"
            :class="['timeline-item', { active: selectedDiary?.id === diary.id }]"
            @click="selectDiary(diary)"
          >
            <div class="timeline-mood" :style="{ backgroundColor: getMoodColor(diary.mood) }">
              {{ getMoodEmoji(diary.mood) }}
            </div>
            <div class="timeline-content">
              <div class="timeline-title">{{ diary.title || '无标题' }}</div>
              <div class="timeline-mood-text">{{ diary.mood }}</div>
              <div class="timeline-time">{{ formatTime(diary.createTime) }}</div>
            </div>
            <el-button
              type="danger"
              text
              circle
              size="small"
              @click.stop="deleteDiary(diary.id)"
              title="删除日记"
              class="delete-diary-btn"
            >
              <el-icon><delete /></el-icon>
            </el-button>
          </div>
        </div>
      </aside>

      <main class="diary-area" style="animation: fadeInUp 0.4s ease-out 0.2s both;">
        <div v-if="isPreview" class="diary-preview">
          <div class="preview-header">
            <div class="preview-mood-section">
              <span class="preview-mood" :style="{ backgroundColor: getMoodColor(selectedDiary.mood) }">
                {{ getMoodEmoji(selectedDiary.mood) }}
              </span>
              <div class="preview-mood-info">
                <h2>{{ selectedDiary.title || '无标题' }}</h2>
                <div class="preview-mood-label">情绪: {{ selectedDiary.mood }}</div>
                <div class="preview-time">{{ formatTime(selectedDiary.createTime) }}</div>
                <div class="preview-score">
                  情绪分数:
                  <span class="score-value">{{ selectedDiary.moodScore }}</span>/10
                </div>
              </div>
            </div>
          </div>
          <div class="preview-content">
            <div v-if="selectedDiary.imageUrl" class="preview-images">
              <div class="image-grid">
                <div v-for="(imageUrl, index) in selectedDiary.imageUrl.split(';')" :key="index" class="image-item">
                  <div v-if="typeof imageUrl === 'string' && imageUrl.trim()" class="preview-image-wrapper" @click="previewImage(imageUrl.trim())">
                    <el-image 
                      :src="imageUrl.trim()" 
                      fit="cover" 
                      class="uploaded-image"
                      :preview-src-list="[imageUrl.trim()]"
                    />
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedDiary.content" class="preview-text markdown-content" v-html="formatContent(selectedDiary.content)"></div>
          </div>

          <!-- 图片预览对话框 -->
          <el-dialog
            v-model="previewDialogVisible"
            :title="'图片预览'"
            top="5vh"
            width="80%"
            append-to-body
            class="image-preview-dialog"
          >
            <div class="dialog-image-container">
              <el-image
                :src="previewImageUrl"
                fit="contain"
                class="dialog-image"
              />
            </div>
          </el-dialog>
        </div>

        <div v-else class="diary-editor">
          <div class="editor-header">
            <h2>记录此刻的心情</h2>
          </div>

          <div class="editor-form">
            <div class="form-group">
              <label>日记标题</label>
              <el-input
                v-model="formData.title"
                placeholder="请输入日记标题..."
                maxlength="100"
                show-word-limit
                class="title-input"
              />
            </div>

            <div class="mood-score-row">
              <div class="form-group">
                <label>选择情绪</label>
                <el-select v-model="formData.mood" placeholder="请选择..." @change="updateMoodScore" style="width: 100%" class="mood-select">
                  <el-option
                    v-for="mood in moodOptions"
                    :key="mood.value"
                    :label="`${mood.emoji} ${mood.label}`"
                    :value="mood.value"
                  />
                </el-select>
              </div>

              <div class="form-group">
                <label>情绪分数</label>
                <div class="score-slider-container">
                  <el-slider
                    v-model="formData.moodScore"
                    :min="1"
                    :max="10"
                    :show-tooltip="false"
                    style="flex: 1"
                    class="mood-slider"
                  />
                  <span class="score-display">{{ formData.moodScore }}/10</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>写点什么吧</label>
              <el-input
                v-model="formData.content"
                type="textarea"
                :rows="10"
                placeholder="记录今天的心情和感受..."
                resize="none"
                class="content-textarea"
              />
              <div class="markdown-hint">支持 Markdown 格式</div>
            </div>

            <div class="form-group">
              <div class="image-upload-area">
                <input
                  type="file"
                  ref="imageInput"
                  accept="image/*"
                  multiple
                  style="display: none"
                  @change="handleImageSelect"
                />
                <div class="image-upload-content">
                  <div class="upload-button-section">
                    <el-button type="primary" @click="handleImageClick" size="large" class="upload-btn">
                      <el-icon><picture /></el-icon> 添加图片
                    </el-button>
                  </div>
                  <div v-if="formData.imageUrls.length > 0" class="image-preview-container">
                    <div class="image-grid">
                      <div v-for="(imageUrl, index) in formData.imageUrls" :key="index" class="image-item">
                        <el-image :src="imageUrl" fit="cover" class="uploaded-image" />
                        <el-button 
                          type="danger" 
                          circle 
                          size="small" 
                          @click="removeImage(index)" 
                          class="remove-image-btn"
                        >
                          <el-icon><close /></el-icon>
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="save-button-section">
                  <el-button type="primary" :loading="isSaving" :disabled="!formData.mood || !formData.title" @click="saveDiary" size="large" class="save-btn">
                    保存日记
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowDown, Plus, Delete, Picture, Close, Edit
} from '@element-plus/icons-vue';
import {
  getEmotionDiaryList,
  addEmotionDiary,
  deleteEmotionDiary,
  logout,
  uploadImage
} from '../api';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || {});

const diaryList = ref([]);
const selectedDiary = ref(null);
const isPreview = ref(false);
const isSaving = ref(false);
const imageInput = ref(null);
const previewDialogVisible = ref(false);
const previewImageUrl = ref('');

const moodOptions = [
  { value: '开心', label: '开心', emoji: '😊', defaultScore: 8 },
  { value: '平静', label: '平静', emoji: '😌', defaultScore: 6 },
  { value: '难过', label: '难过', emoji: '😢', defaultScore: 3 },
  { value: '愤怒', label: '愤怒', emoji: '😠', defaultScore: 2 },
  { value: '焦虑', label: '焦虑', emoji: '😰', defaultScore: 3 },
  { value: '其他', label: '其他', emoji: '🤔', defaultScore: 5 }
];

const formData = ref({
  title: '',
  mood: '',
  moodScore: 5,
  content: '',
  imageUrls: []
});

const pendingImageFiles = ref([]);

const getMoodColor = (mood) => {
  const colors = {
    '开心': '#fbbf24',
    '平静': '#60a5fa',
    '难过': '#94a3b8',
    '愤怒': '#f87171',
    '焦虑': '#fb923c',
    '其他': '#a78bfa'
  };
  return colors[mood] || '#94a3b8';
};

const getMoodEmoji = (mood) => {
  const emojis = {
    '开心': '😊',
    '平静': '😌',
    '难过': '😢',
    '愤怒': '😠',
    '焦虑': '😰',
    '其他': '🤔'
  };
  return emojis[mood] || '🤔';
};

const updateMoodScore = (mood) => {
  const selectedMood = moodOptions.find(m => m.value === mood);
  if (selectedMood) {
    formData.value.moodScore = selectedMood.defaultScore;
  }
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

const formatContent = (content) => {
  if (!content) return '';
  const rendered = marked(content);
  return DOMPurify.sanitize(rendered);
};

const loadDiaryList = async () => {
  try {
    const response = await getEmotionDiaryList(user.value.id);
    if (response.code === 0 && response.data) {
      diaryList.value = response.data || [];
    }
  } catch (error) {
    console.error('加载日记列表失败:', error);
  }
};

const selectDiary = (diary) => {
  selectedDiary.value = diary;
  isPreview.value = true;
};

const resetForm = () => {
  selectedDiary.value = null;
  isPreview.value = false;
  formData.value = {
    title: '',
    mood: '',
    moodScore: 5,
    content: '',
    imageUrls: []
  };
  pendingImageFiles.value = [];
};

const handleImageClick = () => {
  imageInput.value?.click();
};

const handleImageSelect = (event) => {
  const files = event.target.files;
  if (files && files.length > 0) {
    Array.from(files).forEach(file => {
      pendingImageFiles.value.push(file);
      const reader = new FileReader();
      reader.onload = (e) => {
        formData.value.imageUrls.push(e.target.result);
      };
      reader.readAsDataURL(file);
    });
  }
};

const removeImage = (index) => {
  formData.value.imageUrls.splice(index, 1);
  pendingImageFiles.value.splice(index, 1);
  if (imageInput.value) {
    imageInput.value.value = '';
  }
};

const saveDiary = async () => {
  if (!formData.value.title || formData.value.title.trim() === '') {
    ElMessage.warning('请输入日记标题');
    return;
  }
  if (!formData.value.mood) {
    ElMessage.warning('请选择情绪');
    return;
  }

  isSaving.value = true;
  try {
    const imageUrls = [];
    
    for (const file of pendingImageFiles.value) {
      const uploadResponse = await uploadImage(file);
      if (uploadResponse.code === 0 && uploadResponse.data) {
        // 检查 uploadResponse.data 是字符串还是对象
        const imageUrl = typeof uploadResponse.data === 'string' 
          ? uploadResponse.data 
          : (uploadResponse.data.url || uploadResponse.data.fileUrl || '');
        if (imageUrl) {
          imageUrls.push(imageUrl);
        }
      }
    }
    
    const imageUrlString = imageUrls.length > 0 
      ? imageUrls.join(';') 
      : formData.value.imageUrls.join(';');
    
    const response = await addEmotionDiary({
      userId: user.value.id,
      title: formData.value.title,
      mood: formData.value.mood,
      moodScore: formData.value.moodScore,
      content: formData.value.content,
      imageUrl: imageUrlString
    });
    
    if (response.code === 0) {
      ElMessage.success('保存成功');
      await loadDiaryList();
      resetForm();
    } else {
      ElMessage.error(response.message || '保存失败');
    }
  } catch (error) {
    console.error('保存日记失败:', error);
    ElMessage.error('保存失败');
  } finally {
    isSaving.value = false;
  }
};

const deleteDiary = async (diaryId) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇日记吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    const response = await deleteEmotionDiary({ id: diaryId });
    if (response.code === 0) {
      ElMessage.success('删除成功');
      await loadDiaryList();
      if (selectedDiary.value?.id === diaryId) {
        resetForm();
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除日记失败:', error);
    }
  }
};

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

const handleLogout = async () => {
  try {
    await logout();
  } catch (error) {
    console.error('登出失败:', error);
  }
  localStorage.removeItem('user');
  router.push('/');
};

const goToHome = () => {
  router.push('/');
};

const goToChat = () => {
  router.push('/chat');
};
const goToEmotionDiary = () => router.push('/emotion-diary');
const goToEmotionClassroom = () => {
  router.push('/emotion-classroom');
};

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
const previewImage = (imageUrl) => {
  previewImageUrl.value = imageUrl;
  previewDialogVisible.value = true;
};

onMounted(async () => {
  await loadDiaryList();
});
</script>

<style scoped>
.emotion-diary-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
  padding: 12px 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: var(--shadow-soft);
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: transform var(--transition-fast);
}

.nav-left:hover {
  transform: translateX(4px);
}

.nav-logo {
  font-size: 32px;
  animation: pulse 3s ease-in-out infinite;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.nav-center {
  display: flex;
  gap: 8px;
}

.nav-btn {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  padding: 8px 16px;
  transition: all var(--transition-base);
}

.nav-btn:hover {
  color: var(--color-primary);
  background: rgba(255, 107, 157, 0.1);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--color-text-primary);
  padding: 8px 16px;
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
}

.el-dropdown-link:hover {
  background: var(--color-surface-soft);
}

.user-avatar {
  border: 2px solid var(--color-primary-light);
}

.user-name {
  font-weight: 500;
  font-size: 14px;
}

/* 主内容区 */
.diary-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 24px;
  gap: 24px;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* 侧边栏 */
.timeline-sidebar {
  width: 320px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  flex-shrink: 0;
}

.timeline-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.new-diary-btn {
  background: var(--gradient-1);
  border: none;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.new-diary-btn:hover {
  transform: scale(1.1);
  box-shadow: var(--shadow-medium);
}

.timeline-list {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-diary-icon {
  font-size: 48px;
  animation: float 3s ease-in-out infinite;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  position: relative;
  transition: all var(--transition-base);
  border: 1px solid transparent;
}

.timeline-item:hover {
  background: rgba(255, 107, 157, 0.05);
  border-color: rgba(255, 107, 157, 0.15);
  transform: translateX(4px);
}

.timeline-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  border-color: var(--color-primary-light);
}

.timeline-mood {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.timeline-item:hover .timeline-mood {
  transform: scale(1.1);
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-mood-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 2px;
}

.timeline-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 2px;
}

.delete-diary-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: all var(--transition-base);
}

.timeline-item:hover .delete-diary-btn {
  opacity: 1;
}

.delete-diary-btn:hover {
  background: rgba(245, 101, 101, 0.1);
}

/* 日记区域 */
.diary-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.diary-preview,
.diary-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

/* 预览区域 */
.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
}

.preview-mood-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preview-mood {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.preview-mood:hover {
  transform: scale(1.1);
}

.preview-mood-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: var(--color-text-primary);
}

.preview-mood-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.preview-time {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.preview-score {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.score-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
}

.preview-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.preview-images {
  margin-bottom: 24px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.image-item {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  aspect-ratio: 1;
  transition: all var(--transition-base);
}

.image-item:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-medium);
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-lg);
}

.preview-image-wrapper {
  position: relative;
  cursor: pointer;
  transition: transform var(--transition-base);
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.preview-image-wrapper:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-medium);
}

.preview-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-primary);
}

/* 编辑器区域 */
.editor-header {
  padding: 24px 24px 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
}

.editor-header h2 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: var(--color-text-primary);
}

.editor-form {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.mood-score-row {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  margin-top: 16px;
}

.form-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

/* 输入框样式 */
.title-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 12px 16px;
  font-size: 15px;
  transition: all var(--transition-base);
}

.title-input :deep(.el-input__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.mood-select :deep(.el-select__wrapper) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  transition: all var(--transition-base);
}

.mood-select :deep(.el-select__wrapper:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.content-textarea :deep(.el-textarea__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 16px;
  font-size: 15px;
  line-height: 1.6;
  transition: all var(--transition-base);
  resize: none;
}

.content-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.markdown-hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
}

.score-slider-container {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.mood-slider :deep(.el-slider__runway) {
  background-color: rgba(255, 107, 157, 0.1);
  height: 8px;
  border-radius: 4px;
}

.mood-slider :deep(.el-slider__bar) {
  background: var(--gradient-1);
  height: 8px;
  border-radius: 4px;
}

.mood-slider :deep(.el-slider__button) {
  border: 2px solid var(--color-primary);
  background: white;
  width: 20px;
  height: 20px;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.mood-slider :deep(.el-slider__button:hover) {
  transform: scale(1.2);
  box-shadow: var(--shadow-medium);
}

.score-display {
  min-width: 60px;
  text-align: right;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 图片上传区域 */
.image-upload-area {
  width: 100%;
  padding: 8px 0;
}

.image-upload-content {
  margin-bottom: 20px;
  width: 100%;
}

.upload-button-section {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 20px;
}

.upload-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  padding: 12px 24px;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.save-button-section {
  display: flex;
  justify-content: flex-end;
  width: 100%;
}

.save-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  padding: 12px 32px;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.image-preview-container {
  padding: 16px;
  width: 100%;
  background: rgba(255, 107, 157, 0.05);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 107, 157, 0.15);
  margin-bottom: 20px;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.remove-image-btn:hover {
  background: rgba(245, 101, 101, 1);
  transform: scale(1.1);
}

/* 图片预览对话框样式 */
:deep(.image-preview-dialog .el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
}

:deep(.image-preview-dialog .el-dialog__header) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

:deep(.image-preview-dialog .el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.dialog-image-container {
  width: 100%;
  height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-soft);
  border-radius: var(--radius-lg);
}

.dialog-image {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: var(--radius-lg);
}

/* Markdown 内容样式 */
:deep(.markdown-content) h1,
:deep(.markdown-content) h2,
:deep(.markdown-content) h3,
:deep(.markdown-content) h4,
:deep(.markdown-content) h5,
:deep(.markdown-content) h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
  color: var(--color-text-primary);
}

:deep(.markdown-content) h1 { font-size: 1.5em; }
:deep(.markdown-content) h2 { font-size: 1.3em; }
:deep(.markdown-content) h3 { font-size: 1.15em; }
:deep(.markdown-content) h4 { font-size: 1em; }
:deep(.markdown-content) h5 { font-size: 0.9em; }
:deep(.markdown-content) h6 { font-size: 0.8em; }

:deep(.markdown-content) p {
  margin: 0.5em 0;
  line-height: 1.7;
  color: var(--color-text-primary);
}

:deep(.markdown-content) ul,
:deep(.markdown-content) ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
  color: var(--color-text-primary);
}

:deep(.markdown-content) li {
  margin: 0.3em 0;
}

:deep(.markdown-content) code {
  background: rgba(255, 107, 157, 0.1);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: var(--color-text-primary);
}

:deep(.markdown-content) pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 0.5em 0;
}

:deep(.markdown-content) pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

:deep(.markdown-content) blockquote {
  border-left: 3px solid var(--color-primary);
  padding-left: 1em;
  margin: 0.5em 0;
  color: var(--color-text-secondary);
  font-style: italic;
  background: rgba(255, 107, 157, 0.05);
  padding: 12px 16px;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

:deep(.markdown-content) a {
  color: var(--color-primary);
  text-decoration: none;
  transition: all var(--transition-fast);
}

:deep(.markdown-content) a:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

:deep(.markdown-content) strong,
:deep(.markdown-content) b {
  font-weight: 600;
  color: var(--color-text-primary);
}

:deep(.markdown-content) em,
:deep(.markdown-content) i {
  font-style: italic;
}

:deep(.markdown-content) hr {
  border: none;
  border-top: 1px solid rgba(255, 107, 157, 0.15);
  margin: 1em 0;
}

:deep(.markdown-content) table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

:deep(.markdown-content) th,
:deep(.markdown-content) td {
  border: 1px solid rgba(255, 107, 157, 0.15);
  padding: 0.5em;
  text-align: left;
  color: var(--color-text-primary);
}

:deep(.markdown-content) th {
  background: rgba(255, 107, 157, 0.05);
  font-weight: 600;
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

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
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
  .diary-main {
    padding: 16px;
    gap: 16px;
  }
  
  .timeline-sidebar {
    width: 280px;
  }
  
  .preview-content,
  .editor-form {
    padding: 20px;
  }
}

@media (max-width: 992px) {
  .diary-main {
    flex-direction: column;
  }
  
  .timeline-sidebar {
    width: 100%;
    max-height: 300px;
  }
  
  .timeline-list {
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .timeline-item {
    width: calc(50% - 4px);
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 12px 16px;
  }
  
  .nav-center {
    display: none;
  }
  
  .diary-main {
    padding: 12px;
  }
  
  .timeline-sidebar {
    max-height: 250px;
  }
  
  .timeline-item {
    width: 100%;
  }
  
  .mood-score-row {
    flex-direction: column;
    gap: 16px;
  }
  
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }
}

/* 滚动条样式 */
.timeline-list::-webkit-scrollbar,
.preview-content::-webkit-scrollbar,
.editor-form::-webkit-scrollbar {
  width: 6px;
}

.timeline-list::-webkit-scrollbar-track,
.preview-content::-webkit-scrollbar-track,
.editor-form::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.timeline-list::-webkit-scrollbar-thumb,
.preview-content::-webkit-scrollbar-thumb,
.editor-form::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.timeline-list::-webkit-scrollbar-thumb:hover,
.preview-content::-webkit-scrollbar-thumb:hover,
.editor-form::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
