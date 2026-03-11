<template>
  <div class="emotion-diary-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <span class="nav-item" @click="goToChat">情感陪伴</span>
        <span class="nav-item active">情绪日记</span>
        <span class="nav-item">个人中心</span>
        <span class="nav-item">反馈与建议</span>
      </div>
      <div class="nav-right">
        <div class="user-avatar">{{ user.username?.charAt(0) || 'U' }}</div>
        <span class="user-name">{{ user.username }}</span>
        <button @click="handleLogout" class="logout-btn">退出</button>
      </div>
    </nav>

    <div class="diary-main">
      <aside class="timeline-sidebar">
        <div class="timeline-header">
          <h3>我的日记</h3>
          <button @click="resetForm" class="new-diary-btn" title="新建日记">
            ➕
          </button>
        </div>
        <div class="timeline-list">
          <div v-if="diaryList.length === 0" class="empty-diary">
            <span class="empty-diary-icon">📔</span>
            <span class="empty-diary-text">还没有日记</span>
          </div>
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
            <button
              @click.stop="deleteDiary(diary.id)"
              class="delete-diary-btn"
              title="删除日记"
            >
              🗑️
            </button>
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
            <button @click="resetForm" class="edit-btn">✏️ 写新日记</button>
          </div>
          <div class="preview-content">
            <div v-if="selectedDiary.imageUrl" class="preview-image">
              <img :src="selectedDiary.imageUrl" alt="日记图片" />
            </div>
            <div v-if="selectedDiary.content" class="preview-text markdown-content" v-html="formatContent(selectedDiary.content)"></div>
          </div>
        </div>

        <div v-else class="diary-editor">
          <div class="editor-header">
            <h2>记录此刻的心情</h2>
          </div>

          <div class="editor-form">
            <div class="form-group">
              <label>选择情绪</label>
              <select v-model="formData.mood" @change="updateMoodScore" class="mood-select">
                <option value="">请选择...</option>
                <option v-for="mood in moodOptions" :key="mood.value" :value="mood.value">
                  {{ mood.emoji }} {{ mood.label }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label>情绪分数 (1-10)</label>
              <div class="score-slider-container">
                <input
                  type="range"
                  v-model.number="formData.moodScore"
                  min="1"
                  max="10"
                  class="score-slider"
                />
                <span class="score-display">{{ formData.moodScore }}/10</span>
              </div>
            </div>

            <div class="form-group">
              <label>写点什么吧... (支持Markdown)</label>
              <textarea
                v-model="formData.content"
                class="content-textarea"
                placeholder="记录今天的心情和感受..."
                rows="10"
              ></textarea>
            </div>

            <div class="form-group">
              <label>添加图片</label>
              <div class="image-upload-area">
                <input
                  type="file"
                  ref="imageInput"
                  accept="image/*"
                  style="display: none"
                  @change="handleImageSelect"
                />
                <div v-if="!formData.imageUrl" class="upload-placeholder" @click="handleImageClick">
                  <span class="upload-icon">📷</span>
                  <span class="upload-text">点击上传图片</span>
                </div>
                <div v-else class="image-preview-container">
                  <img :src="formData.imageUrl" alt="预览" class="uploaded-image" />
                  <button @click="removeImage" class="remove-image-btn">×</button>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button @click="resetForm" class="cancel-btn">取消</button>
              <button @click="saveDiary" :disabled="isSaving || !formData.mood" class="save-btn">
                {{ isSaving ? '保存中...' : '保存日记' }}
              </button>
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
  imageUrl: ''
});

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
  return emojis[mood] || '📝';
};

const updateMoodScore = () => {
  const mood = moodOptions.find(m => m.value === formData.value.mood);
  if (mood) {
    formData.value.moodScore = mood.defaultScore;
  }
};

const formatTime = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}`;
};

const formatContent = (content) => {
  if (!content) return '';
  try {
    marked.setOptions({
      breaks: true,
      gfm: true,
      smartypants: true
    });
    const htmlContent = marked.parse(content);
    return DOMPurify.sanitize(htmlContent);
  } catch (error) {
    console.error('Markdown渲染错误:', error);
    return content.replace(/\n/g, '<br>');
  }
};

const loadDiaryList = async () => {
  try {
    const response = await getEmotionDiaryList();
    if (response.data.code === 0 && response.data.data) {
      diaryList.value = response.data.data;
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
    imageUrl: ''
  };
};

const handleImageClick = () => {
  imageInput.value.click();
};

const handleImageSelect = async (event) => {
  const file = event.target.files[0];
  if (file) {
    try {
      const response = await uploadImage(file);
      if (response.data.code === 0 && response.data.data) {
        formData.value.imageUrl = response.data.data.fileUrl;
      }
    } catch (error) {
      console.error('上传图片失败:', error);
      alert('图片上传失败，请重试');
    }
  }
};

const removeImage = () => {
  formData.value.imageUrl = '';
  if (imageInput.value) {
    imageInput.value.value = '';
  }
};

const saveDiary = async () => {
  if (!formData.value.mood) {
    alert('请选择情绪');
    return;
  }

  isSaving.value = true;
  try {
    const response = await addEmotionDiary({
      mood: formData.value.mood,
      moodScore: formData.value.moodScore,
      content: formData.value.content,
      imageUrl: formData.value.imageUrl
    });
    if (response.data.code === 0) {
      alert('日记保存成功！');
      await loadDiaryList();
      resetForm();
    }
  } catch (error) {
    console.error('保存日记失败:', error);
    alert('保存失败，请重试');
  } finally {
    isSaving.value = false;
  }
};

const deleteDiary = async (id) => {
  if (!confirm('确定要删除这篇日记吗？删除后将无法恢复！')) return;

  try {
    const response = await deleteEmotionDiary({ id });
    if (response.data.code === 0) {
      alert('删除成功');
      if (selectedDiary.value?.id === id) {
        resetForm();
      }
      await loadDiaryList();
    }
  } catch (error) {
    console.error('删除日记失败:', error);
    alert('删除失败，请重试');
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

onMounted(async () => {
  await loadDiaryList();
});
</script>

<style scoped>
.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

.markdown-content h1 { font-size: 1.5em; }
.markdown-content h2 { font-size: 1.3em; }
.markdown-content h3 { font-size: 1.15em; }
.markdown-content h4 { font-size: 1em; }
.markdown-content h5 { font-size: 0.9em; }
.markdown-content h6 { font-size: 0.8em; }

.markdown-content p {
  margin: 0.5em 0;
  line-height: 1.6;
}

.markdown-content ul,
.markdown-content ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content li {
  margin: 0.25em 0;
}

.markdown-content code {
  background: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.markdown-content pre {
  background: #1f2937;
  color: #e5e7eb;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.markdown-content pre code {
  background: transparent;
  padding: 0;
  color: inherit;
}

.markdown-content blockquote {
  border-left: 4px solid #3b82f6;
  padding-left: 1em;
  margin: 0.5em 0;
  color: #64748b;
  font-style: italic;
}

.markdown-content a {
  color: #3b82f6;
  text-decoration: underline;
}

.markdown-content a:hover {
  color: #2563eb;
}

.markdown-content strong,
.markdown-content b {
  font-weight: 600;
}

.markdown-content em,
.markdown-content i {
  font-style: italic;
}

.markdown-content hr {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 1em 0;
}

.markdown-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

.markdown-content th,
.markdown-content td {
  border: 1px solid #e2e8f0;
  padding: 0.5em;
  text-align: left;
}

.markdown-content th {
  background: #f8fafc;
  font-weight: 600;
}

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

.nav-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-item:hover {
  color: #3b82f6;
  background: #f1f5f9;
}

.nav-item.active {
  color: #3b82f6;
  background: #eff6ff;
  font-weight: 500;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.user-name {
  font-size: 14px;
  color: #334155;
}

.logout-btn {
  padding: 6px 16px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #fee2e2;
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
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.timeline-header h3 {
  margin: 0;
  font-size: 18px;
  color: #334155;
}

.new-diary-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 10px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.new-diary-btn:hover {
  background: #dbeafe;
  transform: scale(1.05);
}

.timeline-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.empty-diary {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
  gap: 8px;
}

.empty-diary-icon {
  font-size: 48px;
}

.empty-diary-text {
  font-size: 14px;
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
  border: none;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-item:hover .delete-diary-btn {
  opacity: 1;
}

.delete-diary-btn:hover {
  background: #fee2e2;
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
  font-size: 24px;
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
  color: #3b82f6;
}

.edit-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.preview-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.preview-image {
  margin-bottom: 24px;
}

.preview-image img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 12px;
  object-fit: contain;
}

.preview-text {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
}

.editor-header {
  padding: 24px 24px 0;
  border-bottom: 1px solid #e2e8f0;
}

.editor-header h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  color: #334155;
}

.editor-form {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 8px;
}

.mood-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 15px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.mood-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.score-slider-container {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-slider {
  flex: 1;
  height: 8px;
  -webkit-appearance: none;
  background: #e2e8f0;
  border-radius: 4px;
  outline: none;
}

.score-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.score-display {
  min-width: 60px;
  text-align: right;
  font-size: 18px;
  font-weight: 600;
  color: #3b82f6;
}

.content-textarea {
  width: 100%;
  padding: 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 15px;
  line-height: 1.6;
  font-family: inherit;
  resize: vertical;
  transition: all 0.2s;
}

.content-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.image-upload-area {
  min-height: 120px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.image-upload-area:hover {
  border-color: #3b82f6;
  background: #f8fafc;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #64748b;
}

.upload-icon {
  font-size: 36px;
}

.upload-text {
  font-size: 14px;
}

.image-preview-container {
  position: relative;
  padding: 12px;
}

.uploaded-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  object-fit: contain;
}

.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #dc2626;
  color: white;
  border: none;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 0;
}

.remove-image-btn:hover {
  background: #b91c1c;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.cancel-btn {
  padding: 12px 28px;
  background: white;
  color: #64748b;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f8fafc;
}

.save-btn {
  padding: 12px 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
