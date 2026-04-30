<template>
  <div class="profile-container">
    <el-container>
      <el-header class="navbar">
        <div class="nav-left">
          <span class="nav-logo">❤️</span>
          <span class="nav-title">青少年情感陪伴智能体</span>
        </div>
        <div class="nav-right">
          <el-button text type="primary" @click="goBack">
            <el-icon><arrow-left /></el-icon>
            返回
          </el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <el-card class="profile-card" shadow="hover">
          <template #header>
            <h2 class="profile-title">个人中心</h2>
          </template>

          <div class="avatar-section">
            <div class="avatar-wrapper">
              <el-avatar v-if="userInfo.avatar" :size="120" :src="userInfo.avatar" class="avatar-img" />
              <el-avatar v-else :size="120" class="avatar-placeholder">
                {{ userInfo.username?.charAt(0) || 'U' }}
              </el-avatar>
              <el-button type="primary" class="avatar-upload-btn">
                <label for="avatar-input" style="cursor: pointer; display: block; width: 100%;">
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

          <el-divider />

          <el-form :model="userInfo" label-width="100px" class="form-section">
            <el-form-item label="用户名">
              <el-input :value="userInfo.username" disabled />
            </el-form-item>

            <el-form-item label="昵称">
              <div class="input-wrapper">
                <el-input 
                  v-model="editNickname" 
                  placeholder="请输入昵称（2-10个字符）" 
                  style="flex: 1"
                  maxlength="10"
                  show-word-limit
                />
                <el-button type="primary" @click="handleUpdateNickname">保存</el-button>
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
            <el-form v-if="showPasswordForm" label-width="100px" class="password-form">
              <el-form-item label="旧密码">
                <el-input
                  v-model="oldPassword"
                  type="password"
                  placeholder="请输入旧密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input
                  v-model="newPassword"
                  type="password"
                  placeholder="请输入新密码（6-16个字符）"
                  show-password
                  maxlength="16"
                />
              </el-form-item>
              <el-form-item label="确认新密码">
                <el-input
                  v-model="confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                  maxlength="16"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdatePassword" style="width: 100%">确认修改</el-button>
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
  
  if (editNickname.value.trim().length < 2 || editNickname.value.trim().length > 10) {
    ElMessage.warning('昵称长度必须在2-10个字符之间');
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

  if (newPassword.value.trim().length < 6 || newPassword.value.trim().length > 16) {
    ElMessage.warning('新密码长度必须在6-20个字符之间');
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
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  height: 64px;
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
  color: #333;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-right .el-button {
  font-size: 16px;
  font-weight: 500;
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 32px 24px;
}

.profile-card {
  border-radius: 16px;
  width: 100%;
  max-width: 600px;
}

.profile-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin: 0px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-img,
.avatar-placeholder {
  border: 4px solid #409eff;
}

.avatar-placeholder {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 48px;
  font-weight: 600;
}

.avatar-upload-btn {
  padding: 8px 24px;
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

.sex-options {
  display: flex;
  gap: 12px;
}

.sex-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 1px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  user-select: none;
}

.sex-option:hover {
  border-color: #409eff;
}

.sex-option.female.active {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.sex-option.male.active {
  border-color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.sex-icon {
  font-size: 20px;
  font-weight: bold;
}

.sex-option.female .sex-icon {
  color: #f56c6c;
}

.sex-option.male .sex-icon {
  color: #409eff;
}

.sex-text {
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.password-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.section-header:hover {
  background: #ecf5ff;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.toggle-icon {
  font-size: 20px;
  color: #409eff;
  transition: transform 0.3s;
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.password-form {
  margin-top: 16px;
}
</style>
