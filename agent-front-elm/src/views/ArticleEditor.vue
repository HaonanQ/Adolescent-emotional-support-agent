<template>
  <div class="editor-container">
    <nav class="navbar">
      <div class="nav-left" @click="handleGoBack" style="cursor: pointer;">
        <el-icon class="back-icon"><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <span class="nav-label">{{ isEdit ? '编辑文章' : '新建文章' }}</span>
      </div>
      <div class="nav-right">
        <el-button type="primary" :loading="saving" @click="handleSave" v-if="!isEdit">
          发布文章
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdate" v-else>
          保存修改
        </el-button>
      </div>
    </nav>

    <div class="editor-main">
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
  background-image: url('../image/bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  gap: 6px;
  cursor: pointer;
  color: #666;
  font-size: 15px;
  transition: color 0.2s;
}

.nav-left:hover {
  color: #409eff;
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
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.editor-main {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  margin-top: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.editor-form {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.form-item {
  margin-bottom: 24px;
  position: relative;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #555;
  margin-bottom: 10px;
}

.title-input {
  width: 100%;
  border: none;
  border-bottom: 2px solid #eee;
  font-size: 26px;
  font-weight: 700;
  color: #222;
  padding: 8px 4px;
  outline: none;
  transition: border-color 0.2s;
  background: transparent;
}

.title-input:focus {
  border-bottom-color: #409eff;
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -22px;
  font-size: 12px;
  color: #bbb;
}

.form-row {
  display: flex;
  gap: 24px;
  margin-top: 22px;
}

.cover-section {
  flex-shrink: 0;
  width: 220px;
}

.cover-upload {
  width: 220px;
  height: 140px;
  border-radius: 10px;
  border: 2px dashed #ddd;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-upload:hover {
  border-color: #409eff;
  background: #f5f9ff;
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #ccc;
  font-size: 13px;
}

.meta-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-input {
  width: 100%;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
  box-sizing: border-box;
}

.summary-input:focus {
  border-color: #409eff;
}

.editor-item {
  margin-top: 16px;
}

.editor-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.toolbar {
  border-bottom: 1px solid #e8e8e8;
}

.editor-content {
  min-height: 400px;
  overflow-y: auto;
}
</style>
