<template>
  <div class="user-management-container">
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
          <el-button class="nav-btn" @click="goToKnowledgeManagement">知识库管理</el-button>
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

    <div class="management-main" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
      <div class="page-header">
        <h2>用户信息管理</h2>
        <p class="header-desc">查看所有注册用户及其情绪状态</p>
      </div>

      <div class="user-list-section">
        <div class="filter-bar">
          <div class="filter-left">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名/昵称"
              clearable
              style="width: 220px"
              class="search-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select
              v-model="statusFilter"
              placeholder="账号状态"
              clearable
              style="width: 140px"
              class="filter-select"
              @change="handleSearch"
            >
              <el-option label="已启用" :value="0" />
              <el-option label="已停用" :value="1" />
            </el-select>
            <el-select
              v-model="sexFilter"
              placeholder="性别"
              clearable
              style="width: 120px"
              class="filter-select"
              @change="handleSearch"
            >
              <el-option label="女生" :value="0" />
              <el-option label="男生" :value="1" />
            </el-select>
            <el-button type="primary" class="search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>  
              <span style="margin-left:6px;">搜索</span>
            </el-button>
            <el-button class="reset-btn" @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table
          :data="userList"
          v-loading="loading"
          stripe
          style="width: 100%"
          empty-text="暂无用户数据"
          row-class-name="user-row"
        >
          <el-table-column label="用户信息" min-width="200">
            <template #default="{ row }">
              <div class="user-info-cell">
                <el-avatar :size="44" :src="row.avatar" v-if="row.avatar" class="user-avatar"></el-avatar>
                <el-avatar :size="44" v-else class="user-avatar">{{ row.username?.charAt(0) || 'U' }}</el-avatar>
                <div class="user-detail">
                  <div class="user-username">
                    {{ row.nickname || row.username }}
                    <el-tag v-if="row.isAdmin === 1" type="danger" size="small" class="admin-tag">管理员</el-tag>
                  </div>
                  <div class="user-id">ID: {{ row.id }}</div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="性别" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.sex === 0" type="danger" size="small" class="table-tag">女生</el-tag>
              <el-tag v-else-if="row.sex === 1" type="primary" size="small" class="table-tag">男生</el-tag>
              <span v-else class="no-mood">-</span>
            </template>
          </el-table-column>

          <el-table-column label="日记数量" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info" class="table-tag">{{ row.diaryCount || 0 }} 篇</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="最新情绪分数" width="150" align="center">
            <template #default="{ row }">
              <span
                v-if="row.latestMoodScore != null"
                class="mood-score-clickable"
                @click="showEmotionHistory(row)"
              >
                <el-tag
                  :type="getMoodTagType(row.latestMoodScore)"
                  size="large"
                  effect="dark"
                  round
                  class="mood-tag"
                >
                  {{ row.latestMoodScore }} 分
                </el-tag>
                <span class="mood-hint">点击查看趋势</span>
              </span>
              <span v-else class="no-mood">用户未更新</span>
            </template>
          </el-table-column>

          <el-table-column label="当前状态" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.latestMood" class="mood-status">{{ row.latestMood }}</span>
              <span v-else class="no-mood">-</span>
            </template>
          </el-table-column>

          <el-table-column label="账号状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'" size="small" class="table-tag">
                {{ row.isDeleted === 1 ? '已停用' : '已启用' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.isAdmin !== 1"
                :type="row.isDeleted === 1 ? 'success' : 'danger'"
                size="small"
                text
                class="action-btn"
                @click="handleToggleStatus(row)"
              >
                {{ row.isDeleted === 1 ? '启用' : '停用' }}
              </el-button>
              <span v-else class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="historyDialogVisible"
      :title="'📊 ' + (currentUser?.nickname || currentUser?.username) + ' 的情绪变化趋势'"
      width="680px"
      class="history-dialog"
    >
      <div class="history-content" v-loading="historyLoading">
        <div v-if="emotionHistory.length > 0" class="history-timeline">
          <div
            v-for="(item, index) in emotionHistory"
            :key="item.id"
            class="history-item"
          >
            <div class="timeline-dot" :class="'mood-level-' + getMoodLevel(item.moodScore)">
              <span class="dot-inner">{{ item.moodScore }}</span>
            </div>
            <div class="timeline-content">
              <div class="history-header">
                <span class="history-mood">{{ item.mood }}</span>
                <el-tag
                  :type="getMoodTagType(item.moodScore)"
                  size="small"
                  effect="dark"
                  round
                >{{ item.moodScore }} 分</el-tag>
              </div>
              <div class="history-date">{{ formatDateFull(item.diaryDate) }}</div>
            </div>
            <div v-if="index < emotionHistory.length - 1" class="timeline-line" :class="'line-mood-' + getMoodLevel(item.moodScore)"></div>
          </div>
        </div>
        <el-empty v-else description="该用户暂无情绪记录" :image-size="80" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Search } from '@element-plus/icons-vue';
import { getUserList, getEmotionHistory, toggleUserStatus, logout } from '../api';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('user')) || null);
const userList = ref([]);
const loading = ref(false);
const historyDialogVisible = ref(false);
const historyLoading = ref(false);
const currentUser = ref(null);
const emotionHistory = ref([]);
const searchKeyword = ref('');
const statusFilter = ref(null);
const sexFilter = ref(null);

/**
 * 加载用户列表
 */
const loadUserList = async () => {
  loading.value = true;
  try {
    const params = {};
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (statusFilter.value !== null && statusFilter.value !== undefined) {
      params.isDeleted = statusFilter.value;
    }
    if (sexFilter.value !== null && sexFilter.value !== undefined) {
      params.sex = sexFilter.value;
    }
    const res = await getUserList(Object.keys(params).length > 0 ? params : null);
    if (res.code === 0) {
      userList.value = res.data || [];
    }
  } catch (error) {
    console.error('加载用户列表失败:', error);
    ElMessage.error('加载用户列表失败');
  } finally {
    loading.value = false;
  }
};

/**
 * 搜索
 */
const handleSearch = () => {
  loadUserList();
};

/**
 * 重置过滤条件
 */
const resetFilters = () => {
  searchKeyword.value = '';
  statusFilter.value = null;
  sexFilter.value = null;
  loadUserList();
};

/**
 * 切换用户状态
 */
const handleToggleStatus = async (row) => {
  const action = row.isDeleted === 1 ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(
      `确定要${action}用户「${row.nickname || row.username}」的账号吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    const res = await toggleUserStatus(row.id);
    if (res.code === 0) {
      ElMessage.success(`${action}成功`);
      loadUserList();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error);
      ElMessage.error('操作失败');
    }
  }
};

/**
 * 显示用户情绪历史
 */
const showEmotionHistory = async (userInfo) => {
  currentUser.value = userInfo;
  historyDialogVisible.value = true;
  historyLoading.value = true;
  emotionHistory.value = [];
  try {
    const res = await getEmotionHistory(userInfo.id);
    if (res.code === 0) {
      emotionHistory.value = res.data || [];
    }
  } catch (error) {
    console.error('加载情绪历史失败:', error);
    ElMessage.error('加载情绪历史失败');
  } finally {
    historyLoading.value = false;
  }
};

/**
 * 根据分数获取标签类型
 */
const getMoodTagType = (score) => {
  if (score <= 3) return 'danger';
  if (score <= 5) return 'warning';
  if (score <= 7) return '';
  return 'success';
};

/**
 * 根据分数获取情绪等级（用于样式）
 */
const getMoodLevel = (score) => {
  if (score <= 3) return 'low';
  if (score <= 5) return 'medium';
  if (score <= 7) return 'good';
  return 'high';
};

/**
 * 格式化日期（简短）
 */
const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

/**
 * 格式化完整日期时间
 */
const formatDateFull = (dateStr) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
};

/**
 * 下拉菜单命令处理
 */
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

/**
 * 退出登录
 */
const handleLogout = async () => {
  try {
    await logout();
  } catch (error) {
    console.error('登出失败:', error);
  }
  localStorage.removeItem('user');
  router.push('/');
};

const goToHome = () => router.push('/');
const goToChat = () => router.push('/chat');
const goToEmotionDiary = () => router.push('/emotion-diary');
const goToEmotionClassroom = () => router.push('/emotion-classroom');
const goToKnowledgeManagement = () => router.push('/knowledge-management');
const goToFeedback = () => {
  if (user.value?.isAdmin === 1) {
    router.push('/feedback-management');
  } else {
    router.push('/feedback');
  }
};

onMounted(() => {
  loadUserList();
});
</script>

<style scoped>
.user-management-container {
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
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  box-shadow: var(--shadow-soft);
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
  font-size: 14px;
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
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.management-main {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-xl);
  padding: 24px;
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.page-header {
  margin-bottom: 24px;
  text-align: center;
  padding: 24px 0;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  border-radius: var(--radius-lg);
  margin: 0 -24px 24px;
}

.page-header h2 {
  font-size: 26px;
  color: var(--color-text-primary);
  margin-bottom: 6px;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-desc {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.user-list-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 10px 16px;
  transition: all var(--transition-base);
}

.search-input :deep(.el-input__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.filter-select :deep(.el-select__wrapper) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  transition: all var(--transition-base);
}

.filter-select :deep(.el-select__wrapper:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.search-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.reset-btn {
  background: transparent;
  border: 2px solid var(--color-primary-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
}

.reset-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-detail {
  display: flex;
  flex-direction: column;
}

.user-username {
  font-weight: 600;
  color: var(--color-text-primary);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-tag {
  margin-left: 4px;
  border-radius: var(--radius-full);
}

.user-id {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.mood-score-clickable {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  gap: 4px;
  transition: all var(--transition-base);
}

.mood-score-clickable:hover {
  transform: scale(1.05);
}

.mood-tag {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  transition: transform var(--transition-base);
  border-radius: var(--radius-full);
  padding: 4px 12px;
}

.mood-score-clickable:hover .mood-tag {
  transform: scale(1.08);
}

.mood-hint {
  font-size: 11px;
  color: var(--color-primary);
  transition: all var(--transition-base);
}

.no-mood {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.mood-status {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.no-action {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.table-tag {
  border-radius: var(--radius-full);
}

.action-btn {
  transition: all var(--transition-base);
  border-radius: var(--radius-full);
}

.action-btn:hover {
  background: rgba(255, 107, 157, 0.05);
}

.history-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.history-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
}

.history-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  color: var(--color-text-primary);
}

.history-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 500px;
  overflow-y: auto;
}

.history-content {
  width: 100%;
}

.history-timeline {
  position: relative;
  padding: 8px 0;
  max-height: 420px;
  overflow-y: auto;
}

.history-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0;
  padding-left: 8px;
  animation: fadeInUp 0.3s ease-out;
}

.timeline-dot {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  z-index: 1;
  transition: transform var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.timeline-dot:hover {
  transform: scale(1.12);
  box-shadow: var(--shadow-medium);
}

.mood-level-low { background: linear-gradient(135deg, #f56c6c, #e03535); }
.mood-level-medium { background: linear-gradient(135deg, #e6a23c, #d97706); }
.mood-level-good { background: linear-gradient(135deg, #67c23a, #4a9d2a); }
.mood-level-high { background: linear-gradient(135deg, #409eff, #2078d4); }

.dot-inner {
  color: #fff;
}

.timeline-content {
  flex: 1;
  padding-top: 4px;
  background: white;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
}

.history-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.history-mood {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text-primary);
}

.history-date {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.timeline-line {
  position: absolute;
  left: 29px;
  top: 54px;
  width: 2px;
  height: calc(100% - 12px);
  border-radius: 1px;
}

.line-mood-low { background: #f56c6c; opacity: 0.35; }
.line-mood-medium { background: #e6a23c; opacity: 0.35; }
.line-mood-good { background: #67c23a; opacity: 0.35; }
.line-mood-high { background: #409eff; opacity: 0.35; }

:deep(.user-row:hover > td) {
  background: rgba(255, 107, 157, 0.05) !important;
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
  .management-main {
    max-width: 1000px;
    padding: 20px;
  }

  .page-header {
    margin: 0 -20px 20px;
  }
}

@media (max-width: 992px) {
  .management-main {
    max-width: 100%;
    padding: 16px;
  }

  .filter-bar {
    flex-direction: column;
    gap: 16px;
  }

  .filter-left {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 12px 16px;
  }

  .nav-center {
    display: none;
  }

  .management-main {
    margin: 16px;
    padding: 16px;
  }

  .page-header {
    margin: 0 -16px 16px;
    padding: 16px 0;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .filter-left {
    flex-wrap: wrap;
  }

  .search-input {
    width: 100% !important;
  }

  .filter-select {
    flex: 1;
  }

  .history-dialog :deep(.el-dialog) {
    width: 95% !important;
  }
}

/* 滚动条样式 */
.management-main::-webkit-scrollbar,
.history-timeline::-webkit-scrollbar {
  width: 6px;
}

.management-main::-webkit-scrollbar-track,
.history-timeline::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.management-main::-webkit-scrollbar-thumb,
.history-timeline::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.management-main::-webkit-scrollbar-thumb:hover,
.history-timeline::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
