<template>
  <div class="user-management-container">
    <nav class="navbar">
      <div class="nav-left" @click="goToHome" style="cursor: pointer;">
        <span class="nav-logo">❤️</span>
        <span class="nav-title">青少年情感陪伴智能体</span>
      </div>
      <div class="nav-center">
        <el-button text @click="goToChat">情感陪伴</el-button>
        <el-button text @click="goToEmotionDiary">情绪日记</el-button>
        <el-button text @click="goToEmotionClassroom">情感课堂</el-button>
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

    <div class="management-main">
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
              @change="handleSearch"
            >
              <el-option label="女生" :value="0" />
              <el-option label="男生" :value="1" />
            </el-select>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>  
              <span style="margin-left:6px;">搜索</span>
            </el-button>
            <el-button @click="resetFilters">重置</el-button>
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
                <el-avatar :size="44" :src="row.avatar" v-if="row.avatar"></el-avatar>
                <el-avatar :size="44" v-else>{{ row.username?.charAt(0) || 'U' }}</el-avatar>
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
              <el-tag v-if="row.sex === 0" type="danger" size="small">女生</el-tag>
              <el-tag v-else-if="row.sex === 1" type="primary" size="small">男生</el-tag>
              <span v-else class="no-mood">-</span>
            </template>
          </el-table-column>

          <el-table-column label="日记数量" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.diaryCount || 0 }} 篇</el-tag>
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
              <el-tag :type="row.isDeleted === 1 ? 'danger' : 'success'" size="small">
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

onMounted(() => {
  loadUserList();
});
</script>

<style scoped>
.user-management-container {
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
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
  color: #303133;
}

.nav-center {
  display: flex;
  gap: 16px;
}

.nav-center .el-button {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
}

.nav-center .el-button:hover {
  color: #409eff;
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
  color: #303133;
  font-size: 14px;
}

.user-name {
  font-weight: 500;
  font-size: 16px;
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
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.1);
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 6px;
}

.header-desc {
  color: #909399;
  font-size: 14px;
}

.user-list-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
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
  color: #303133;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-tag {
  margin-left: 4px;
}

.user-id {
  font-size: 12px;
  color: #c0c4cc;
}

.mood-score-clickable {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  gap: 4px;
}

.mood-tag {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  transition: transform 0.2s;
}

.mood-score-clickable:hover .mood-tag {
  transform: scale(1.08);
}

.mood-hint {
  font-size: 11px;
  color: #409eff;
}

.no-mood {
  color: #c0c4cc;
  font-size: 13px;
}

.mood-status {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.no-action {
  color: #c0c4cc;
  font-size: 13px;
}

.history-dialog :deep(.el-dialog__body) {
  padding: 16px 24px 24px;
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
  transition: transform 0.2s;
}

.timeline-dot:hover {
  transform: scale(1.12);
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
}

.history-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-mood {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.history-date {
  font-size: 12px;
  color: #909399;
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
  background-color: #f0f7ff !important;
}
</style>
