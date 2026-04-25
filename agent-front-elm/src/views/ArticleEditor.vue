<template>
  <div class="editor-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>

    <nav class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="handleGoBack" style="cursor: pointer;">
        <el-icon class="back-icon"><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <span class="nav-label">{{ isEdit ? '编辑文章' : '新建文章' }}</span>
      </div>
      <div class="nav-right">
        <el-button type="primary" :loading="saving" @click="handleSave" v-if="!isEdit" class="save-btn">
          发布文章
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdate" v-else class="save-btn">
          保存修改
        </el-button>
      </div>
    </nav>

    <div class="editor-main" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
      <div class="editor-form">
        <!-- 标题 -->
        <div class="form-item">
          <input
            v-model="form.title"
            class="title-input"
            placeholder="请输入文章标题..."
            maxlength="100"
          />
          <span class="char-count">{{ form.title.length }}/100</span>
        </div>

        <!-- 封面图 + 摘要 + 分类 -->
        <div class="form-row">
          <div class="cover-section">
            <label class="form-label">封面图片</label>
            <div class="cover-upload" @click="triggerCoverUpload">
              <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
              <div v-else class="cover-placeholder">
                <el-icon :size="32"><plus /></el-icon>
                <span>上传封面</span>
              </div>
            </div>
            <input
              ref="coverInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleCoverUpload"
            />
          </div>

          <div class="meta-section">
            <div class="form-item">
              <label class="form-label">文章摘要</label>
              <textarea
                v-model="form.summary"
                class="summary-input"
                placeholder="请输入文章摘要（用于列表展示）..."
                rows="4"
                maxlength="200"
              ></textarea>
              <span class="char-count">{{ form.summary?.length || 0 }}/200</span>
            </div>

            <!-- <div class="form-item">
              <label class="form-label">文章分类</label>
              <el-input
                v-model="form.category"
                placeholder="如：情绪管理、人际关系、自我成长..."
                clearable
                style="width: 100%"
              />
            </div> -->
          </div>
        </div>

        <!-- 富文本编辑器 -->
        <div class="form-item editor-item">
          <label class="form-label">文章内容</label>
          <div class="editor-wrapper">
            <Toolbar
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
              class="toolbar"
            />
            <Editor
              :defaultConfig="editorConfig"
              mode="default"
              v-model="form.content"
              class="editor-content"
              @onCreated="handleEditorCreated"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, shallowRef } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import '@wangeditor/editor/dist/css/style.css';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { getAdminArticleDetail, addArticle, updateArticle, uploadImage } from '../api/index';

const router = useRouter();
const route = useRoute();

const editorRef = shallowRef(null);
const coverInputRef = ref(null);
const saving = ref(false);
const isEdit = ref(false);
const hasChanges = ref(false);

const form = reactive({
  id: null,
  title: '',
  content: '',
  coverImage: '',
  summary: '',
  category: '',
  status: 1
});

/**
 * 工具栏配置
 */
const toolbarConfig = {
  excludeKeys: [
    'fullScreen',
    'group-video'
  ]
};

/**
 * 编辑器配置（支持图片上传）
 */
const editorConfig = {
  placeholder: '开始编写你的文章...',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        try {
          const res = await uploadImage(file);
          if (res.code === 0 && res.data) {
            insertFn(res.data.fileUrl, file.name, '');
          } else {
            ElMessage.error('图片上传失败');
          }
        } catch (error) {
          console.error('图片上传错误:', error);
          ElMessage.error('图片上传失败');
        }
      }
    }
  }
};

onMounted(() => {
  const articleId = route.query.id;
  if (articleId) {
    isEdit.value = true;
    loadArticle(articleId);
  }

  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
  if (editorRef.value) {
    editorRef.value.destroy();
  }
});

/**
 * 编辑器创建完成回调
 */
const handleEditorCreated = (editor) => {
  editorRef.value = editor;
};

/**
 * 加载已有文章数据（编辑模式）
 */
const loadArticle = async (id) => {
  try {
    const res = await getAdminArticleDetail(id);
    if (res.code === 0 && res.data) {
      form.id = res.data.id;
      form.title = res.data.title || '';
      form.content = res.data.content || '';
      form.coverImage = res.data.coverImage || '';
      form.summary = res.data.summary || '';
      form.category = res.data.category || '';
      form.status = res.data.status ?? 1;
    }
  } catch (error) {
    console.error('加载文章失败:', error);
    ElMessage.error('加载文章失败');
  }
};

/**
 * 触发封面上传
 */
const triggerCoverUpload = () => {
  coverInputRef.value.click();
};

/**
 * 处理封面图片上传
 */
const handleCoverUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }

  try {
    const res = await uploadImage(file);
    if (res.code === 0 && res.data) {
      form.coverImage = res.data.fileUrl;
      hasChanges.value = true;
    }
  } catch (error) {
    console.error('封面上传失败:', error);
    ElMessage.error('封面上传失败');
  }

  event.target.value = '';
};

/**
 * 创建/发布新文章
 */
const handleSave = async () => {
  if (!validateForm()) return;

  saving.value = true;
  try {
    const res = await addArticle({
      title: form.title,
      content: form.content,
      coverImage: form.coverImage,
      summary: form.summary,
      category: form.category,
      status: form.status
    });

    if (res.code === 0) {
      ElMessage.success('发布成功');
      hasChanges.value = false;
      router.push('/emotion-classroom');
    } else {
      ElMessage.error(res.message || '发布失败');
    }
  } catch (error) {
    console.error('发布失败:', error);
    ElMessage.error('发布失败');
  } finally {
    saving.value = false;
  }
};

/**
 * 更新已有文章
 */
const handleUpdate = async () => {
  if (!validateForm()) return;

  saving.value = true;
  try {
    const res = await updateArticle({
      id: form.id,
      title: form.title,
      content: form.content,
      coverImage: form.coverImage,
      summary: form.summary,
      category: form.category,
      status: form.status
    });

    if (res.code === 0) {
      ElMessage.success('保存成功');
      hasChanges.value = false;
      router.push('/emotion-classroom');
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

/**
 * 表单校验
 */
const validateForm = () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入文章标题');
    return false;
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入文章内容');
    return false;
  }
  return true;
};

/**
 * 页面关闭前提示未保存的修改
 */
const handleBeforeUnload = (e) => {
  if (hasUnsavedChanges()) {
    e.preventDefault();
    e.returnValue = '';
  }
};

/**
 * 检测是否有未保存的修改
 */
const hasUnsavedChanges = () => {
  return form.title || form.content || form.summary || form.coverImage;
};

/**
 * 返回操作（带确认提示）
 */
const handleGoBack = async () => {
  if (hasUnsavedChanges() && hasChanges.value) {
    try {
      await ElMessageBox.confirm(
        '当前有未保存的内容，确定要离开吗？',
        '提示',
        { confirmButtonText: '离开', cancelButtonText: '取消', type: 'warning' }
      );
      router.back();
    } catch {
      // 用户点击了取消
    }
  } else {
    router.back();
  }
};
</script>

<style scoped>
.editor-container {
  min-height: 100vh;
  background: var(--color-background);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 导航栏 */
.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.back-icon {
  font-size: 18px;
}

.nav-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.nav-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.save-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  padding: 8px 16px;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.editor-main {
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

.editor-form {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-soft);
}

.form-item {
  margin-bottom: 24px;
  position: relative;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.title-input {
  width: 100%;
  border: none;
  border-bottom: 2px solid rgba(255, 107, 157, 0.15);
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary);
  padding: 12px 8px;
  outline: none;
  transition: border-color var(--transition-base);
  background: transparent;
  font-family: inherit;
}

.title-input:focus {
  border-bottom-color: var(--color-primary);
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -24px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.form-row {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.cover-section {
  flex-shrink: 0;
  width: 220px;
}

.cover-upload {
  width: 220px;
  height: 140px;
  border-radius: var(--radius-lg);
  border: 2px dashed rgba(255, 107, 157, 0.2);
  cursor: pointer;
  overflow: hidden;
  transition: all var(--transition-base);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-soft);
}

.cover-upload:hover {
  border-color: var(--color-primary);
  background: rgba(255, 107, 157, 0.05);
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--radius-lg);
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary);
  font-size: 13px;
  transition: all var(--transition-base);
}

.cover-upload:hover .cover-placeholder {
  color: var(--color-primary);
}

.meta-section {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-input {
  width: 100%;
  border: 2px solid rgba(255, 107, 157, 0.15);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  resize: vertical;
  outline: none;
  transition: all var(--transition-base);
  font-family: inherit;
  box-sizing: border-box;
  background: var(--color-surface-soft);
}

.summary-input:focus {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
  background: white;
}

.editor-item {
  margin-top: 32px;
}

.editor-wrapper {
  border: 2px solid rgba(255, 107, 157, 0.15);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-base);
}

.editor-wrapper:hover {
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-soft);
}

.toolbar {
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  background: var(--color-surface-soft);
}

.editor-content {
  min-height: 500px;
  overflow-y: auto;
  background: white;
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
  .editor-main {
    max-width: 800px;
    padding: 20px;
  }
}

@media (max-width: 992px) {
  .editor-main {
    max-width: 100%;
    margin: 24px;
    padding: 20px;
  }

  .form-row {
    flex-direction: column;
  }

  .cover-section {
    width: 100%;
  }

  .cover-upload {
    width: 100%;
    height: 200px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }

  .nav-center {
    display: none;
  }

  .editor-main {
    margin: 16px;
    padding: 16px;
  }

  .editor-form {
    padding: 24px;
  }

  .title-input {
    font-size: 20px;
  }

  .cover-upload {
    height: 160px;
  }

  .editor-content {
    min-height: 400px;
  }
}

/* 滚动条样式 */
.editor-main::-webkit-scrollbar,
.editor-content::-webkit-scrollbar {
  width: 6px;
}

.editor-main::-webkit-scrollbar-track,
.editor-content::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.editor-main::-webkit-scrollbar-thumb,
.editor-content::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.editor-main::-webkit-scrollbar-thumb:hover,
.editor-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
