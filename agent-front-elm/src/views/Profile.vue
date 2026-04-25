<template>
  <div class="profile-container">
    <!-- 装饰背景元素 -->
    <div class="decorative-circle circle-1" style="top: -150px; right: -150px; opacity: 0.3;"></div>
    <div class="decorative-circle circle-2" style="bottom: -100px; left: -100px; opacity: 0.3;"></div>
    
    <el-container>
      <el-header class="navbar" style="animation: fadeInDown 0.4s ease-out;">
        <div class="nav-left" @click="goToHome" style="cursor: pointer;">
          <span class="nav-logo">❤️</span>
          <span class="nav-title">青少年情感陪伴智能体</span>
        </div>
        <div class="nav-right">
          <el-button class="back-btn" @click="goBack">
            <el-icon><arrow-left /></el-icon>
            返回
          </el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <el-card class="profile-card" shadow="hover" style="animation: fadeInUp 0.4s ease-out 0.1s both;">
          <template #header>
            <h2 class="profile-title">个人中心</h2>
          </template>

          <div class="avatar-section">
            <div class="avatar-wrapper">
              <div class="avatar-container">
                <el-avatar v-if="userInfo.avatar" :size="120" :src="userInfo.avatar" class="avatar-img" />
                <el-avatar v-else :size="120" class="avatar-placeholder">
                  {{ userInfo.username?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="avatar-overlay">
                  <el-button type="primary" class="avatar-upload-btn">
                    <label for="avatar-input" style="cursor: pointer; display: block; width: 100%; height: 100%;">
                      更换头像
                    </label>
                  </el-button>
                  <input
                    type="file"
                    id="avatar-input"
                    accept="image/*"
                    style="display: none"
                    @change="handleAvatarChange"
                  />
                </div>
              </div>
              <div class="user-info">
                <div class="username">{{ userInfo.username }}</div>
                <div class="nickname" v-if="userInfo.nickname">{{ userInfo.nickname }}</div>
              </div>
            </div>
          </div>

          <el-divider />

          <el-form :model="userInfo" label-width="100px" class="form-section">
            <el-form-item label="用户名">
              <el-input :value="userInfo.username" disabled class="disabled-input" />
            </el-form-item>

            <el-form-item label="昵称">
              <div class="input-wrapper">
                <el-input v-model="editNickname" placeholder="请输入昵称" style="flex: 1" class="nickname-input" />
                <el-button type="primary" @click="handleUpdateNickname" class="save-btn">保存</el-button>
              </div>
            </el-form-item>

            <el-form-item label="性别">
              <div class="sex-options">
                <div
                  :class="['sex-option', 'female', { active: userInfo.sex === 0 }]"
                  @click="handleUpdateSex(0)"
                >
                  <span class="sex-icon">♀</span>
                  <span class="sex-text">女</span>
                </div>
                <div
                  :class="['sex-option', 'male', { active: userInfo.sex === 1 }]"
                  @click="handleUpdateSex(1)"
                >
                  <span class="sex-icon">♂</span>
                  <span class="sex-text">男</span>
                </div>
              </div>
            </el-form-item>
          </el-form>

          <el-divider />

          <div class="password-section">
            <div class="section-header" @click="showPasswordForm = !showPasswordForm">
              <h3 class="section-title">修改密码</h3>
              <el-icon :class="['toggle-icon', { rotated: showPasswordForm }]">
                <arrow-down />
              </el-icon>
            </div>
            <el-form v-if="showPasswordForm" label-width="100px" class="password-form" style="animation: fadeInDown 0.3s ease-out;">
              <el-form-item label="旧密码">
                <el-input
                  v-model="oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                  show-password
                  class="password-input"
                />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input
                  v-model="newPassword"
                  type="password"
                  placeholder="请输入新密码"
                  show-password
                  class="password-input"
                />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input
                  v-model="confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                  class="password-input"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdatePassword" class="update-password-btn">确认修改</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, ArrowDown } from '@element-plus/icons-vue';
import {
  getCurrentUserInfo,
  updateNickname,
  updatePassword,
  updateAvatar,
  updateSex,
  uploadImage
} from '../api';

const router = useRouter();
const userInfo = ref({});
const editNickname = ref('');
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const showPasswordForm = ref(false);

onMounted(async () => {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user) {
    router.push('/login');
    return;
  }

  userInfo.value = user;
  editNickname.value = user.nickname || '';

  try {
    const response = await getCurrentUserInfo();
    if (response.code === 0 && response.data) {
      userInfo.value = response.data;
      editNickname.value = response.data.nickname || '';
      localStorage.setItem('user', JSON.stringify(response.data));
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
});

const goBack = () => {
  router.back();
};

const goToHome = () => {
  router.push('/');
};

const handleAvatarChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  try {
    const uploadResponse = await uploadImage(file);
    if (uploadResponse.code === 0 && uploadResponse.data) {
      const avatarUrl = uploadResponse.data.fileUrl;
      const response = await updateAvatar(avatarUrl);
      if (response.code === 0 && response.data) {
        userInfo.value = response.data;
        localStorage.setItem('user', JSON.stringify(response.data));
        ElMessage.success('头像更新成功！');
      }
    }
  } catch (error) {
    console.error('更新头像失败:', error);
    ElMessage.error('更新头像失败，请重试');
  }
};

const handleUpdateNickname = async () => {
  if (!editNickname.value.trim()) {
    ElMessage.warning('昵称不能为空');
    return;
  }

  try {
    const response = await updateNickname(editNickname.value);
    if (response.code === 0 && response.data) {
      userInfo.value = response.data;
      localStorage.setItem('user', JSON.stringify(response.data));
      ElMessage.success('昵称更新成功！');
    }
  } catch (error) {
    console.error('更新昵称失败:', error);
    ElMessage.error('更新昵称失败，请重试');
  }
};

const handleUpdateSex = async (sex) => {
  try {
    const response = await updateSex(sex);
    if (response.code === 0 && response.data) {
      userInfo.value = response.data;
      localStorage.setItem('user', JSON.stringify(response.data));
      ElMessage.success('性别更新成功！');
    }
  } catch (error) {
    console.error('更新性别失败:', error);
    ElMessage.error('更新性别失败，请重试');
  }
};

const handleUpdatePassword = async () => {
  if (!oldPassword.value || !newPassword.value || !confirmPassword.value) {
    ElMessage.warning('请填写完整的密码信息');
    return;
  }

  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  try {
    const response = await updatePassword(oldPassword.value, newPassword.value);
    if (response.code === 0) {
      ElMessage.success('密码修改成功！');
      oldPassword.value = '';
      newPassword.value = '';
      confirmPassword.value = '';
    }
  } catch (error) {
    console.error('修改密码失败:', error);
    ElMessage.error('修改密码失败，请检查旧密码是否正确');
  }
};
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
  box-shadow: var(--shadow-soft);
  border-bottom: 1px solid rgba(255, 107, 157, 0.1);
  height: 64px;
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

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  border: none;
  background: transparent;
  border-radius: var(--radius-full);
  padding: 8px 16px;
  transition: all var(--transition-base);
}

.back-btn:hover {
  color: var(--color-primary);
  background: rgba(255, 107, 157, 0.1);
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 32px 24px;
}

.profile-card {
  border-radius: var(--radius-xl);
  width: 100%;
  max-width: 600px;
  box-shadow: var(--shadow-soft);
  border: 1px solid rgba(255, 107, 157, 0.1);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.profile-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0px;
  background: var(--gradient-1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  padding: 24px 0;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  margin: 0 -24px 24px;
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.avatar-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-img,
.avatar-placeholder {
  border: 4px solid var(--color-primary-light);
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-base);
}

.avatar-container:hover .avatar-img,
.avatar-container:hover .avatar-placeholder {
  transform: scale(1.05);
  box-shadow: var(--shadow-medium);
}

.avatar-placeholder {
  background: var(--gradient-1);
  color: white;
  font-size: 48px;
  font-weight: 600;
}

.avatar-overlay {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  opacity: 0;
  transition: all var(--transition-base);
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-lg);
  padding: 4px;
  box-shadow: var(--shadow-soft);
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
  transform: translateX(-50%) translateY(10px);
}

.avatar-upload-btn {
  padding: 8px 24px;
  font-weight: 500;
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
}

.avatar-upload-btn:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-medium);
}

.user-info {
  text-align: center;
}

.username {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.nickname {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.form-section {
  margin-top: 24px;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

/* 输入框样式 */
.disabled-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 12px 16px;
  font-size: 15px;
  background: var(--color-surface-soft);
  color: var(--color-text-tertiary);
}

.nickname-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 12px 16px;
  font-size: 15px;
  transition: all var(--transition-base);
}

.nickname-input :deep(.el-input__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.save-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  padding: 0 24px;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.save-btn:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-medium);
}

.sex-options {
  display: flex;
  gap: 16px;
}

.sex-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 2px solid rgba(255, 107, 157, 0.15);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-base);
  user-select: none;
  background: white;
  box-shadow: var(--shadow-soft);
}

.sex-option:hover {
  border-color: var(--color-primary-light);
  transform: translateY(-2px);
  box-shadow: var(--shadow-medium);
}

.sex-option.female.active {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
  box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.1);
}

.sex-option.male.active {
  border-color: var(--color-primary);
  background: rgba(255, 107, 157, 0.1);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.sex-icon {
  font-size: 24px;
  font-weight: bold;
}

.sex-option.female .sex-icon {
  color: #f56c6c;
}

.sex-option.male .sex-icon {
  color: var(--color-primary);
}

.sex-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.password-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.05) 0%, rgba(167, 139, 250, 0.05) 100%);
  border-radius: var(--radius-lg);
  transition: all var(--transition-base);
  border: 1px solid rgba(255, 107, 157, 0.1);
}

.section-header:hover {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1) 0%, rgba(167, 139, 250, 0.1) 100%);
  transform: translateY(-2px);
  box-shadow: var(--shadow-soft);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.toggle-icon {
  font-size: 20px;
  color: var(--color-primary);
  transition: all var(--transition-base);
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.password-form {
  margin-top: 20px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 107, 157, 0.1);
  box-shadow: var(--shadow-soft);
}

.password-input :deep(.el-input__inner) {
  border-radius: var(--radius-lg);
  border: 2px solid rgba(255, 107, 157, 0.15);
  padding: 12px 16px;
  font-size: 15px;
  transition: all var(--transition-base);
}

.password-input :deep(.el-input__inner:focus) {
  border-color: var(--color-primary-light);
  box-shadow: 0 0 0 4px rgba(255, 107, 157, 0.1);
}

.update-password-btn {
  background: var(--gradient-1);
  border: none;
  border-radius: var(--radius-lg);
  padding: 12px;
  width: 100%;
  font-weight: 500;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-soft);
}

.update-password-btn:hover {
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
@media (max-width: 768px) {
  .navbar {
    padding: 0 16px;
  }
  
  .main-content {
    padding: 20px 16px;
  }
  
  .profile-card {
    margin: 0;
  }
  
  .avatar-section {
    margin: 0 -16px 24px;
  }
  
  .input-wrapper {
    flex-direction: column;
  }
  
  .sex-options {
    flex-direction: column;
  }
  
  .sex-option {
    justify-content: center;
  }
  
  .password-form {
    padding: 16px;
  }
}

/* 滚动条样式 */
.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-track {
  background: rgba(255, 107, 157, 0.05);
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-thumb {
  background: rgba(255, 107, 157, 0.3);
  border-radius: 3px;
  transition: background var(--transition-base);
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 107, 157, 0.5);
}
</style>
