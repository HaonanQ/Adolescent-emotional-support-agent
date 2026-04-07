<template>
  <div class="emotion-diary-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button text @click="goToChat">情感陪伴</el-button>
        <el-button text type="primary">情绪日记</el-button>
        <el-button text @click="goToEmotionClassroom">情感课堂</el-button>
        <el-button text>反馈与建议</el-button>
      </div>
      <div class="nav-right">
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar :size="36" :src="user.avatar" v-if="user.avatar"></el-avatar>
            <el-avatar :size="36" v-else>{{ user.username?.charAt(0) || 'U' }}</el-avatar>
            <span class="user-name">{{ user.username }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="userManage" v-if="user.isAdmin === 1">用户管理</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <div class="diary-main">
      <aside class="timeline-sidebar">
        <div class="timeline-header">
          <h3>我的日记</h3>
          <el-button type="primary" circle @click="resetForm" title="新建日记">
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
              <div class="timeline-mood-text">{{ diary.mood }}</div>
              <div class="timeline-time">{{ formatTime(diary.createTime) }}</div>
              <div class="timeline-score">情绪分: {{ diary.moodScore }}/10</div>
            </div>
            <el-button
              type="danger"
              text
              circle
              size="large"
              @click.stop="deleteDiary(diary.id)"
              title="删除日记"
              class="delete-diary-btn"
            >
              <el-icon><delete /></el-icon>
            </el-button>
          </div>
        </div>
      </aside>

      <main class="diary-area">
        <div v-if="isPreview" class="diary-preview">
          <div class="preview-header">
            <div class="preview-mood-section">
              <span class="preview-mood" :style="{ backgroundColor: getMoodColor(selectedDiary.mood) }">
                {{ getMoodEmoji(selectedDiary.mood) }}
              </span>
              <div class="preview-mood-info">
                <h2>{{ selectedDiary.mood }}</h2>
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
            <div class="mood-score-row">
              <div class="form-group">
                <label>选择情绪</label>
                <el-select v-model="formData.mood" placeholder="请选择..." @change="updateMoodScore" style="width: 100%">
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
              />
              <div style="font-size: 12px; color: #909399; margin-top: 4px;">支持 Markdown 格式</div>
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
                    <el-button type="primary" @click="handleImageClick" size="large">
                      <!-- <el-icon><picture /></el-icon> -->
                      添加图片
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
                  <el-button type="primary" :loading="isSaving" :disabled="!formData.mood" @click="saveDiary" size="large">
                    保存日记
                  </el-button>
                </div>
              </div>
            </div>
            <!-- <div class="form-actions">
              <el-button type="primary" :loading="isSaving" :disabled="!formData.mood" @click="saveDiary" size="large">
                保存日记
              </el-button>
            </div> -->
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

const goToEmotionClassroom = () => {
  router.push('/emotion-classroom');
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
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
  color: #334155;
}

.nav-center {
  display: flex;
  gap: 16px;
}

.nav-center .el-button {
  font-size: 16px;
  font-weight: 500;
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
  color: #333;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
}

.diary-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.timeline-sidebar {
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.timeline-header h3 {
  margin: 0;
  font-size: 18px;
  color: #334155;
}

.timeline-list {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
}

.empty-diary-icon {
  font-size: 48px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  position: relative;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.timeline-item:hover {
  background: #f8fafc;
}

.timeline-item.active {
  background: #eff6ff;
  border-color: #bfdbfe;
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-mood-text {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 4px;
}

.timeline-time {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.timeline-score {
  font-size: 11px;
  color: #64748b;
}

.delete-diary-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.timeline-item:hover .delete-diary-btn {
  opacity: 1;
}

.diary-area {
  flex: 1;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.diary-preview,
.diary-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
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
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.preview-mood-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #334155;
}

.preview-time {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 4px;
}

.preview-score {
  font-size: 14px;
  color: #64748b;
}

.score-value {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
}

.preview-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.preview-image {
  margin-bottom: 24px;
}

.preview-image :deep(.el-image) {
  max-width: 100%;
  max-height: 400px;
  border-radius: 12px;
  object-fit: cover;
}

.preview-text {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
}

.editor-header {
  padding: 24px 24px 0;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.editor-header h2 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #334155;
}

.editor-form {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.mood-score-row {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
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
  color: #334155;
}

.score-slider-container {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.score-display {
  min-width: 60px;
  text-align: right;
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

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

.save-button-section {
  display: flex;
  justify-content: flex-end;
  width: 100%;
}

.image-preview-container {
  padding: 16px;
  width: 100%;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  aspect-ratio: 1;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 预览图片样式 */
.preview-image-wrapper {
  position: relative;
  cursor: pointer;
  transition: transform 0.2s;
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff00;
  border-radius: 8px;
  overflow: hidden;
  object-fit: cover;
}

.preview-image-wrapper:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 16px rgb(255, 255, 255);
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
  transition: all 0.2s;
}

/* 图片预览对话框样式 */
.dialog-image-container {
  width: 100%;
  height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 8px;
}

.dialog-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
}

.form-actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
  width: 100%;
}

:deep(.markdown-content) h1,
:deep(.markdown-content) h2,
:deep(.markdown-content) h3,
:deep(.markdown-content) h4,
:deep(.markdown-content) h5,
:deep(.markdown-content) h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

:deep(.markdown-content) h1 { font-size: 1.5em; }
:deep(.markdown-content) h2 { font-size: 1.3em; }
:deep(.markdown-content) h3 { font-size: 1.15em; }
:deep(.markdown-content) h4 { font-size: 1em; }
:deep(.markdown-content) h5 { font-size: 0.9em; }
:deep(.markdown-content) h6 { font-size: 0.8em; }

:deep(.markdown-content) p {
  margin: 0.5em 0;
  line-height: 1.6;
}

:deep(.markdown-content) ul,
:deep(.markdown-content) ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

:deep(.markdown-content) li {
  margin: 0.25em 0;
}

:deep(.markdown-content) code {
  background: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

:deep(.markdown-content) pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5em 0;
}

:deep(.markdown-content) pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

:deep(.markdown-content) blockquote {
  border-left: 4px solid #409eff;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #64748b;
  font-style: italic;
}

:deep(.markdown-content) a {
  color: #409eff;
  text-decoration: underline;
}

:deep(.markdown-content) a:hover {
  color: #66b1ff;
}

:deep(.markdown-content) strong,
:deep(.markdown-content) b {
  font-weight: 600;
}

:deep(.markdown-content) em,
:deep(.markdown-content) i {
  font-style: italic;
}

:deep(.markdown-content) hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 1em 0;
}

:deep(.markdown-content) table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

:deep(.markdown-content) th,
:deep(.markdown-content) td {
  border: 1px solid #e2e8f0;
  padding: 0.5em;
  text-align: left;
}

:deep(.markdown-content) th {
  background: #f8fafc;
  font-weight: 600;
}
</style>
