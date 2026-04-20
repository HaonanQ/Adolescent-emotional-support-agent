<template>
  <div class="classroom-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button text @click="goToChat">情感陪伴</el-button>
        <el-button text @click="goToEmotionDiary">情绪日记</el-button>
        <el-button text type="primary">情感课堂</el-button>
        <el-button text @click="goToKnowledgeManagement" v-if="user?.isAdmin === 1">知识库管理</el-button>
        <el-button text>反馈与建议</el-button>
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

    <div class="classroom-main">
      <div class="page-header">
        <h1 class="page-title">情感课堂</h1>
        <p class="page-desc">在这里，我们用温暖的文章陪伴你成长，每一篇都是为你精心准备的礼物</p>
        <el-button
          v-if="isAdmin"
          type="primary"
          class="new-article-btn"
          @click="goToEditor()"
        >
          新建文章
        </el-button>
      </div>
      <div class="article-list" v-loading="loading">
        <el-empty v-if="!loading && articles.length === 0" description="暂无新文章" :image-size="120">
          <template #image>
            <span class="empty-icon">📚</span>
          </template>
        </el-empty>

        <div
          v-for="article in articles"
          :key="article.id"
          :class="['article-card', { 'invisible-card': isAdmin && article.status !== 1 }]"
          @click="goToDetail(article.id)"
        >
          <div class="article-cover" v-if="article.coverImage">
            <img :src="article.coverImage" :alt="article.title" />
          </div>
          <div class="article-content-wrapper">
            <h2 class="article-title">{{ article.title }}</h2>
            <p class="article-summary" v-if="article.summary">{{ article.summary }}</p>
            <div class="article-meta">
              <div class="meta-left">
                <el-avatar :size="24" class="author-avatar" :src="article.authorAvatar">
                  {{ (article.authorName || '管理员').charAt(0) }}
                </el-avatar>
                <span class="author-name">{{ article.authorName || '管理员' }}</span>
                <el-tag v-if="isAdmin && article.status !== 1" size="small" type="info" class="status-tag">不可见</el-tag>
              </div>
              <div class="meta-right">
                <!-- 管理员操作按钮 -->
                <div class="admin-actions" v-if="isAdmin" @click.stop>
                  <el-tooltip content="编辑" placement="top">
                    <el-button text circle size="large" @click="goToEditor(article.id)">
                      <el-icon><edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="article.status === 1 ? '设为不可见' : '设为可见'" placement="top">
                    <el-button
                      text
                      circle
                      size="large"
                      :class="article.status === 1 ? 'btn-warning' : 'btn-success'"
                      @click="toggleStatus(article)"
                    >
                      <el-icon ><hide /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button text circle size="large" type="danger" @click="handleDelete(article)">
                      <el-icon><delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
                <span class="read-count">
                  <el-icon><view /></el-icon>
                  阅读数：{{ article.readCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDown, View, Plus, Edit, Delete, Hide } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getArticleList, getAdminArticleList, deleteArticle, updateArticle } from '../api/index';

const router = useRouter();
const user = ref(null);
const articles = ref([]);
const loading = ref(false);

/**
 * 判断当前用户是否为管理员
 */
const isAdmin = computed(() => {
  return user.value && user.value.isAdmin === 1;
});

onMounted(() => {
  try {
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      user.value = JSON.parse(storedUser);
    }
  } catch (e) {
    console.error('解析用户信息失败:', e);
    user.value = null;
  }
  loadArticles();
});

/**
 * 加载文章列表（管理员加载全部，普通用户只加载可见的）
 */
const loadArticles = async () => {
  loading.value = true;
  try {
    let res;
    if (isAdmin.value) {
      res = await getAdminArticleList();
    } else {
      res = await getArticleList();
    }
    console.log('文章列表API返回:', res);
    if (res && res.code === 0) {
      articles.value = res.data || [];
    } else {
      console.error('API返回错误:', res);
      articles.value = [];
    }
  } catch (error) {
    console.error('加载文章列表失败:', error);
    ElMessage.error('加载文章列表失败: ' + (error.message || '未知错误'));
    articles.value = [];
  } finally {
    loading.value = false;
  }
};

/**
 * 根据内容估算阅读时长（按每分钟300字计算）
 */
const calcReadDuration = (content) => {
  if (!content) return '0:00';
  const textLength = content.replace(/<[^>]+>/g, '').length;
  const minutes = Math.floor(textLength / 300);
  const seconds = Math.floor((textLength % 300) / 5);
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
};

/**
 * 切换文章可见性
 */
const toggleStatus = async (article) => {
  const newStatus = article.status === 1 ? 0 : 1;
  const actionText = newStatus === 1 ? '发布（设为可见）' : '下架（设为不可见）';

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}该文章吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );

    await updateArticle({ id: article.id, status: newStatus });
    ElMessage.success(actionText + '成功');
    loadArticles();
  } catch (err) {
    if (err !== 'cancel') {
      console.error('操作失败:', err);
    }
  }
};

/**
 * 删除文章
 */
const handleDelete = async (article) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文章「${article.title}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }
    );

    await deleteArticle(article.id);
    ElMessage.success('删除成功');
    loadArticles();
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除失败:', err);
    }
  }
};

const goToHome = () => router.push('/');
const goToChat = () => router.push('/chat');
const goToEmotionDiary = () => router.push('/emotion-diary');
const goToEmotionClassroom = () => {
  router.push('/emotion-classroom');
};
const goToDetail = (id) => {
  router.push({ path: '/article-detail', query: { id } });
};
const goToKnowledgeManagement = () => {
  router.push('/knowledge-management');
};
/**
 * 跳转到编辑器页面
 * @param {number} id - 文章ID，不传则新建
 */
const goToEditor = (id) => {
  if (id) {
    router.push({ path: '/article-editor', query: { id } });
  } else {
    router.push('/article-editor');
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
    localStorage.removeItem('user');
    user.value = null;
    ElMessage.success('退出登录成功');
    router.push('/');
  }
};
</script>

<style scoped>
.classroom-container {
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
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
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

.empty-icon {
  font-size: 64px;
}

.classroom-main {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  margin-top: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.page-header {
  text-align: center;
  margin-bottom: 20px;
  position: relative;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 12px;
}

.page-desc {
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
}

.new-article-btn {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 0px;
}

.article-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  gap: 20px;
  padding: 20px;
}

.invisible-card {
  opacity: 0.7;
  border: 1px dashed #ddd;
}

.article-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.article-card:hover .article-title {
  color: #409eff;
}

.article-cover {
  position: relative;
  width: 200px;
  min-width: 200px;
  height: 140px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.read-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.article-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  margin-bottom: 10px;
  transition: color 0.2s;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 11px;
}

.author-name {
  font-size: 13px;
  color: #999;
}

.status-tag {
  margin-left: 4px;
}

.meta-right {
  display: flex;
  align-items: center;
  gap: 1px;
}

.read-count {
  display: flex;
  align-items: center;
  gap: 1px;
  font-size: 13px;
  color: #bbb;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 0px;
  opacity: 0;
  transition: opacity 0.2s;
}

.admin-actions .el-button {
  padding: 3px;
  margin: 0;
}

.admin-actions .btn-warning {
  color: #e6a23c;
}

.admin-actions .btn-warning:hover {
  color: #ebb563;
}

.admin-actions .btn-success {
  color: #67c23a;
}

.admin-actions .btn-success:hover {
  color: #85ce61;
}

.article-card:hover .admin-actions {
  opacity: 1;
}
</style>
