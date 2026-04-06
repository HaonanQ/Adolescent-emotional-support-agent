<template>
  <div class="detail-container">
    <nav class="navbar">
      <div class="nav-left" @click="goBack" style="cursor: pointer;">
        <el-icon class="back-icon"><arrow-left /></el-icon>
        <span>返回</span>
      </div>
      <div class="nav-center">
        <span class="nav-label">情感课堂</span>
      </div>
      <div class="nav-right"></div>
    </nav>

    <div class="detail-main" v-loading="loading">
      <div class="article-full" v-if="article">
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-info">
            <div class="info-left">
              <el-avatar :size="28" class="author-avatar">
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
        <el-button type="primary" @click="goBack">返回列表</el-button>
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
  background: #fff;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #fff;
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

.detail-main {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 20px 60px;
}

.article-full {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.article-header {
  margin-bottom: 28px;
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: #222;
  line-height: 1.4;
  margin-bottom: 16px;
}

.article-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
}

.author-name {
  font-size: 14px;
  color: #666;
}

.divider {
  color: #ddd;
  font-size: 14px;
}

.create-time {
  font-size: 13px;
  color: #aaa;
}

.info-right {
  display: flex;
  align-items: center;
}

.read-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #bbb;
}

.cover-image {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 28px;
}

.cover-image img {
  width: 100%;
  display: block;
}

.article-body {
  font-size: 16px;
  line-height: 1.9;
  color: #333;
  word-wrap: break-word;
}

.article-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
  display: block;
}

.article-body :deep(p) {
  margin: 12px 0;
}

.article-body :deep(h1),
.article-body :deep(h2),
.article-body :deep(h3) {
  margin: 24px 0 12px;
  font-weight: 600;
  color: #222;
}

.article-body :deep(h2) {
  font-size: 22px;
}

.article-body :deep(h3) {
  font-size: 19px;
}

.article-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 12px 20px;
  margin: 16px 0;
  background: #f5f7fa;
  border-radius: 0 8px 8px 0;
  color: #555;
}

.article-body :deep(strong) {
  font-weight: 600;
  color: #222;
}

.article-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.article-body :deep(a:hover) {
  text-decoration: underline;
}

.article-body :deep(ul),
.article-body :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.article-body :deep(li) {
  margin: 6px 0;
}
</style>
