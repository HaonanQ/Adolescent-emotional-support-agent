<template>
  <div class="classroom-container">
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
        <el-dropdown @command="handleCommand" v-if="user">
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

    <div class="classroom-main">
      <div class="page-header" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
        <h1 class="page-title">情感课堂</h1>
        <p class="page-desc">在这里，我们用温暖的文章陪伴你成长，每一篇都是为你精心准备的礼物</p>
        <el-button
          v-if="isAdmin"
          type="primary"
          class="new-article-btn"
          @click="goToEditor()"
        >
          <el-icon><plus /></el-icon> 新建文章
        </el-button>
      </div>
      <div class="article-list" v-loading="loading">
        <el-empty v-if="!loading && articles.length === 0" description="暂无新文章" :image-size="120">
          <template #image>
            <span class="empty-icon">📚</span>
          </template>
        </el-empty>

        <div
          v-for="(article, index) in articles"
          :key="article.id"
          :class="['article-card', { 'invisible-card': isAdmin && article.status !== 1 }]"
          @click="goToDetail(article.id)"
          style="animation: fadeInUp 0.4s ease-out " + (0.2 + index * 0.1) + "s both"
        >
          <div class="article-cover" v-if="article.coverImage">
            <img :src="article.coverImage" :alt="article.title" />
            <div class="read-duration">{{ calcReadDuration(article.content) }}</div>
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
                    <el-button text circle size="small" @click="goToEditor(article.id)" class="admin-action-btn">
                      <el-icon><edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip :content="article.status === 1 ? '设为不可见' : '设为可见'" placement="top">
                    <el-button
                      text
                      circle
                      size="small"
                      :class="['admin-action-btn', article.status === 1 ? 'btn-warning' : 'btn-success']"
                      @click="toggleStatus(article)"
                    >
                      <el-icon ><hide /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button text circle size="small" type="danger" @click="handleDelete(article)" class="admin-action-btn">
                      <el-icon><delete /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
                <span class="read-count">
                  <el-icon><view /></el-icon>
                  {{ article.readCount || 0 }} 阅读
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
const goToFeedback = () => {
  if (user.value?.isAdmin === 1) {
    router.push('/feedback-management');
  } else {
    router.push('/feedback');
  }
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
  cursor: pointer;
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

.empty-icon {
  font-size: 64px;
  animation: float 3s ease-in-out infinite;
}

.classroom-main {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  margin-top: 24px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
  padding: 24px 0;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  border-radius: var(--radius-lg);
  margin: 0 -32px 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  max-width: 600px;
  margin: 0 auto;
}

.new-article-btn {
  position: absolute;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  padding: 10px 20px;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.new-article-btn:hover {
  transform: translateY(-50%) scale(1.05);
  box-shadow: var(--shadow-medium);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  background: white;
  border-radius: var(--radius-xl);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  display: flex;
  gap: 24px;
  padding: 24px;
  border: 1px solid rgba(255, 107, 157, 0.1);
}

.invisible-card {
  opacity: 0.7;
  border: 1px dashed rgba(255, 107, 157, 0.3);
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-medium);
  border-color: var(--color-primary-light);
}

.article-card:hover .article-title {
  color: var(--color-primary);
}

.article-cover {
  position: relative;
  width: 220px;
  min-width: 220px;
  height: 160px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.article-card:hover .article-cover {
  transform: scale(1.02);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all var(--transition-base);
}

.read-duration {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.article-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.article-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
  margin-bottom: 12px;
  transition: all var(--transition-base);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-avatar {
  background: var(--gradient-1);
  color: white;
  font-size: 11px;
  border: 1px solid var(--color-primary-light);
}

.author-name {
  font-size: 13px;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.status-tag {
  margin-left: 6px;
  border-radius: var(--radius-full);
}

.meta-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.read-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0;
  transition: all var(--transition-base);
}

.admin-action-btn {
  transition: all var(--transition-base);
  border-radius: var(--radius-full);
}

.admin-action-btn:hover {
  background: var(--color-surface-soft);
  transform: scale(1.1);
}

.admin-actions .btn-warning {
  color: #e6a23c;
}

.admin-actions .btn-warning:hover {
  color: #ebb563;
  background: rgba(230, 162, 60, 0.1);
}

.admin-actions .btn-success {
  color: #67c23a;
}

.admin-actions .btn-success:hover {
  color: #85ce61;
  background: rgba(103, 194, 58, 0.1);
}

.article-card:hover .admin-actions {
  opacity: 1;
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
  .classroom-main {
    max-width: 900px;
    padding: 24px;
  }
  
  .page-header {
    margin: 0 -24px 24px;
  }
  
  .new-article-btn {
    right: 24px;
  }
  
  .article-cover {
    width: 200px;
    min-width: 200px;
    height: 140px;
  }
}

@media (max-width: 992px) {
  .classroom-main {
    max-width: 768px;
  }
  
  .article-card {
    flex-direction: column;
    gap: 16px;
  }
  
  .article-cover {
    width: 100%;
    min-width: 100%;
    height: 200px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 12px 16px;
  }
  
  .nav-center {
    display: none;
  }
  
  .classroom-main {
    margin-top: 16px;
    margin-bottom: 16px;
    padding: 16px;
  }
  
  .page-header {
    margin: 0 -16px 24px;
    padding: 20px 0;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .new-article-btn {
    position: static;
    transform: none;
    margin-top: 16px;
  }
  
  .article-card {
    padding: 16px;
  }
  
  .article-cover {
    height: 160px;
  }
}

/* 滚动条样式 */
.classroom-main::-webkit-scrollbar {
  width: 6px;
}

.classroom-main::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.classroom-main::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.classroom-main::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
