<template>
  <div class="detail-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>

    <nav class="navbar" style="animation: fadeInDown 0.4s ease-out;">
      <div class="nav-left" @click="goBack" style="cursor: pointer;">
        <el-icon class="back-icon"><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <span class="nav-label">情感课堂</span>
      </div>
      <div class="nav-right"></div>
    </nav>

    <div class="detail-main" v-loading="loading" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
      <div class="article-full" v-if="article">
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-info">
            <div class="info-left">
              <el-avatar :size="28" class="author-avatar" :src="article.authorAvatar">
                {{ (article.authorName || '管理员').charAt(0) }}
              </el-avatar>
              <span class="author-name">{{ article.authorName || '管理员' }}</span>
              <span class="divider">·</span>
              <span class="create-time">{{ formatTime(article.createTime) }}</span>
            </div>
            <div class="info-right">
              <span class="read-count">
                <el-icon><view /></el-icon>
                {{ article.readCount || 0 }} 阅读
              </span>
            </div>
          </div>
        </div>

        <div class="cover-image" v-if="article.coverImage">
          <img :src="article.coverImage" :alt="article.title" />
        </div>

        <div class="article-body" v-html="article.content"></div>
      </div>

      <el-empty v-if="!loading && !article" description="文章不存在或已下架" :image-size="120">
        <el-button type="primary" class="back-btn" @click="goBack">返回列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getArticleDetail, getAdminArticleDetail } from '../api/index';

const router = useRouter();
const route = useRoute();
const article = ref(null);
const loading = ref(false);
const user = ref(null);

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
  }
  
  const id = route.query.id;
  if (id) {
    loadArticle(id);
  } else {
    ElMessage.error('缺少文章ID');
    goBack();
  }
});

/**
 * 加载文章详情
 */
const loadArticle = async (id) => {
  loading.value = true;
  try {
    let res;
    if (isAdmin.value) {
      res = await getAdminArticleDetail(id);
    } else {
      res = await getArticleDetail(id);
    }
    if (res.code === 0 && res.data) {
      article.value = res.data;
      document.title = res.data.title + ' - 情感课堂';
    } else {
      ElMessage.error(res.message || '加载文章失败');
    }
  } catch (error) {
    console.error('加载文章详情失败:', error);
    ElMessage.error('加载文章详情失败');
  } finally {
    loading.value = false;
  }
};

/**
 * 格式化时间
 */
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

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.detail-container {
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

.detail-main {
  max-width: 720px;
  margin: 32px auto;
  padding: 32px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.article-full {
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-header {
  margin-bottom: 32px;
  text-align: center;
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.4;
  margin-bottom: 20px;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  padding: 0 20px;
}

.article-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid rgba(255, 107, 157, 0.1);
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.info-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  flex: 1;
}

.author-avatar {
  background: var(--gradient-1);
  color: #fff;
  font-size: 12px;
  border: 2px solid var(--color-primary-light);
}

.author-name {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.divider {
  color: rgba(255, 107, 157, 0.3);
  font-size: 14px;
}

.create-time {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.info-right {
  display: flex;
  align-items: center;
}

.read-count {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-tertiary);
  padding: 4px 12px;
  background: var(--color-surface-soft);
  border-radius: var(--radius-full);
  transition: all var(--transition-base);
}

.read-count:hover {
  background: var(--color-surface);
  color: var(--color-text-secondary);
}

.cover-image {
  width: 100%;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin: 32px 0;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.cover-image:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.cover-image img {
  width: 100%;
  display: block;
  transition: transform var(--transition-base);
}

.cover-image:hover img {
  transform: scale(1.02);
}

.article-body {
  font-size: 16px;
  line-height: 1.9;
  color: var(--color-text-primary);
  word-wrap: break-word;
  padding: 0 20px;
}

.article-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-lg);
  margin: 20px 0;
  display: block;
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.article-body :deep(img:hover) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.article-body :deep(p) {
  margin: 16px 0;
  text-align: justify;
}

.article-body :deep(h1),
.article-body :deep(h2),
.article-body :deep(h3) {
  margin: 28px 0 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.article-body :deep(h2) {
  font-size: 22px;
  padding-bottom: 8px;
  border-bottom: 2px solid rgba(255, 107, 157, 0.1);
}

.article-body :deep(h3) {
  font-size: 19px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.article-body :deep(blockquote) {
  border-left: 4px solid var(--color-primary);
  padding: 16px 20px;
  margin: 20px 0;
  background: rgba(255, 107, 157, 0.05);
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  color: var(--color-text-secondary);
  font-style: italic;
}

.article-body :deep(strong) {
  font-weight: 600;
  color: var(--color-text-primary);
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.article-body :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
  transition: all var(--transition-base);
  position: relative;
}

.article-body :deep(a:hover) {
  color: var(--color-primary-light);
  text-decoration: none;
}

.article-body :deep(a::after) {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--gradient-1);
  transform: scaleX(0);
  transition: transform var(--transition-base);
  transform-origin: left;
}

.article-body :deep(a:hover::after) {
  transform: scaleX(1);
}

.article-body :deep(ul),
.article-body :deep(ol) {
  padding-left: 28px;
  margin: 16px 0;
}

.article-body :deep(li) {
  margin: 8px 0;
  position: relative;
}

.article-body :deep(ul li::before) {
  content: '•';
  color: var(--color-primary);
  font-weight: bold;
  position: absolute;
  left: -20px;
}

.back-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
  margin-top: 16px;
}

.back-btn:hover {
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
  .detail-main {
    max-width: 600px;
    padding: 28px;
  }
}

@media (max-width: 992px) {
  .detail-main {
    max-width: 100%;
    margin: 24px;
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }

  .nav-center {
    display: none;
  }

  .detail-main {
    margin: 16px;
    padding: 20px;
  }

  .article-title {
    font-size: 24px;
    padding: 0 12px;
  }

  .article-body {
    padding: 0 12px;
    font-size: 15px;
  }

  .article-info {
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .info-left {
    justify-content: center;
  }
}

/* 滚动条样式 */
.detail-main::-webkit-scrollbar {
  width: 6px;
}

.detail-main::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.detail-main::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.detail-main::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
